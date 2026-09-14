package com.opencircle.session;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {

    private final AuthSessionRepository sessions;
    private final SessionRefreshTokenRepository refreshTokens;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHasher tokenHasher;
    private final SessionProperties properties;
    private final Clock clock;

    SessionService(
            AuthSessionRepository sessions,
            SessionRefreshTokenRepository refreshTokens,
            RefreshTokenGenerator tokenGenerator,
            RefreshTokenHasher tokenHasher,
            SessionProperties properties,
            Clock clock
    ) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public IssuedSession create(AppUser user, String userAgent) {
        Instant now = Instant.now(clock);
        Instant sessionExpiresAt = now.plus(properties.getAbsoluteLifetimeDays(), ChronoUnit.DAYS);
        Instant refreshTokenExpiresAt = nextRefreshTokenExpiry(now, sessionExpiresAt);

        AuthSession session = sessions.save(new AuthSession(user, userAgent, now, sessionExpiresAt));
        String rawToken = tokenGenerator.generate();
        refreshTokens.save(new SessionRefreshToken(
                session,
                tokenHasher.hash(rawToken),
                now,
                refreshTokenExpiresAt
        ));

        return new IssuedSession(session.getId(), rawToken, refreshTokenExpiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshedSession refresh(String rawToken) {
        String tokenHash = tokenHasher.hash(rawToken);
        AuthSession session = sessions.findForUpdateByRefreshTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        SessionRefreshToken token = refreshTokens.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now(clock);

        if (token.getUsedAt() != null) {
            session.revoke(now, SessionRevocationReason.REFRESH_TOKEN_REUSE);
            throw new InvalidRefreshTokenException();
        }

        if (!session.isActive(now) || !token.isActive(now)) {
            throw new InvalidRefreshTokenException();
        }

        IssuedSession rotated = rotateToken(session, token, now);

        return new RefreshedSession(
                session.getUser().getId(),
                session.getId(),
                rotated.refreshToken(),
                rotated.refreshTokenExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public List<SessionDetails> getActiveSessions(UUID userId, UUID currentSessionId) {
        if (userId == null || currentSessionId == null) {
            throw new IllegalArgumentException("User and current session are required");
        }

        return sessions.findActiveByUserId(userId, currentSessionId, Instant.now(clock)).stream()
                .map(session -> new SessionDetails(
                        session.getId(),
                        session.getUserAgent(),
                        session.getId().equals(currentSessionId),
                        session.getCreatedAt(),
                        session.getLastUsedAt(),
                        session.getInactiveAt(),
                        session.getExpiresAt()
                ))
                .toList();
    }

    @Transactional
    public IssuedSession rotateCurrentAndRevokeOthers(
            UUID userId,
            UUID currentSessionId,
            SessionRevocationReason reason
    ) {
        if (userId == null || currentSessionId == null || reason == null) {
            throw new IllegalArgumentException("User, current session, and reason are required");
        }

        Instant now = Instant.now(clock);
        AuthSession session = sessions.findOwnedForUpdate(currentSessionId, userId)
                .orElseThrow(CurrentSessionUnavailableException::new);
        SessionRefreshToken token = refreshTokens.findFirstBySessionAndUsedAtIsNull(session)
                .orElseThrow(CurrentSessionUnavailableException::new);

        if (!session.isActive(now) || !token.isActive(now)) {
            throw new CurrentSessionUnavailableException();
        }

        IssuedSession rotated = rotateToken(session, token, now);
        sessions.revokeOtherActiveByUserId(userId, currentSessionId, now, reason);
        return rotated;
    }

    @Transactional
    public void revokeOwned(UUID userId, UUID sessionId, SessionRevocationReason reason) {
        if (userId == null || sessionId == null || reason == null) {
            throw new IllegalArgumentException("User, session, and reason are required");
        }

        AuthSession session = sessions.findOwnedForUpdate(sessionId, userId)
                .orElseThrow(SessionNotFoundException::new);
        session.revoke(Instant.now(clock), reason);
    }

    @Transactional
    public void revokeCurrent(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = tokenHasher.hash(rawToken);
        sessions.findForUpdateByRefreshTokenHash(tokenHash)
                .ifPresent(session -> session.revoke(
                        Instant.now(clock),
                        SessionRevocationReason.LOGOUT
                ));
    }

    @Transactional
    public int revokeAll(UUID userId, SessionRevocationReason reason) {
        if (userId == null || reason == null) {
            throw new IllegalArgumentException("User and revocation reason are required");
        }

        return sessions.revokeAllActiveByUserId(userId, Instant.now(clock), reason);
    }

    @Transactional
    public int cleanupExpiredAndRevoked() {
        Instant cutoff = Instant.now(clock).minus(properties.getCleanupRetentionDays(), ChronoUnit.DAYS);
        return sessions.deleteExpiredOrRevokedBefore(cutoff, cutoff);
    }

    private Instant nextRefreshTokenExpiry(Instant issuedAt, Instant sessionExpiresAt) {
        Instant inactivityExpiry = issuedAt.plus(properties.getInactivityTimeoutDays(), ChronoUnit.DAYS);
        return inactivityExpiry.isBefore(sessionExpiresAt) ? inactivityExpiry : sessionExpiresAt;
    }

    private IssuedSession rotateToken(
            AuthSession session,
            SessionRefreshToken currentToken,
            Instant now
    ) {
        currentToken.markUsed(now);
        refreshTokens.flush();
        session.recordUse(now);

        String rawToken = tokenGenerator.generate();
        Instant refreshTokenExpiresAt = nextRefreshTokenExpiry(now, session.getExpiresAt());
        refreshTokens.save(new SessionRefreshToken(
                session,
                tokenHasher.hash(rawToken),
                now,
                refreshTokenExpiresAt
        ));

        return new IssuedSession(session.getId(), rawToken, refreshTokenExpiresAt);
    }
}
