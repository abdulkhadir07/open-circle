package com.opencircle.session;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

        return new IssuedSession(rawToken, refreshTokenExpiresAt);
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

        token.markUsed(now);
        refreshTokens.flush();
        session.recordUse(now);

        String rotatedRawToken = tokenGenerator.generate();
        Instant refreshTokenExpiresAt = nextRefreshTokenExpiry(now, session.getExpiresAt());
        refreshTokens.save(new SessionRefreshToken(
                session,
                tokenHasher.hash(rotatedRawToken),
                now,
                refreshTokenExpiresAt
        ));

        return new RefreshedSession(
                session.getUser().getId(),
                rotatedRawToken,
                refreshTokenExpiresAt
        );
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
}
