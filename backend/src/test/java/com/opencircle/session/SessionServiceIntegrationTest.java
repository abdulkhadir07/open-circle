package com.opencircle.session;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class SessionServiceIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");

    @Autowired private SessionService sessionService;
    @Autowired private AuthSessionRepository sessions;
    @Autowired private SessionRefreshTokenRepository refreshTokens;
    @Autowired private RefreshTokenHasher tokenHasher;
    @Autowired private UserService userService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private RefreshTokenGenerator tokenGenerator;
    @MockitoBean private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(NOW);
    }

    @Test
    @Transactional
    void createsSessionWithoutPersistingRawBearerCredential() {
        AppUser user = createUser("create");
        when(tokenGenerator.generate()).thenReturn("first-raw-refresh-token");

        IssuedSession issued = sessionService.create(user, "  Test Browser  ");

        assertThat(issued.refreshToken()).isEqualTo("first-raw-refresh-token");
        assertThat(issued.refreshTokenExpiresAt()).isEqualTo(Instant.parse("2026-09-20T12:00:00Z"));

        SessionRefreshToken persisted = refreshTokens
                .findByTokenHash(tokenHasher.hash("first-raw-refresh-token"))
                .orElseThrow();
        assertThat(persisted.getTokenHash()).doesNotContain("first-raw-refresh-token");
        assertThat(persisted.getSession().getUserAgent()).isEqualTo("Test Browser");

        Integer rawTokenRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_refresh_tokens WHERE token_hash = ?",
                Integer.class,
                "first-raw-refresh-token"
        );
        assertThat(rawTokenRows).isZero();
    }

    @Test
    @Transactional
    void refreshConsumesCurrentTokenAndIssuesOneReplacement() {
        AppUser user = createUser("rotate");
        when(tokenGenerator.generate()).thenReturn("first-token", "second-token");
        IssuedSession issued = sessionService.create(user, null);

        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        RefreshedSession refreshed = sessionService.refresh(issued.refreshToken());

        assertThat(refreshed.userId()).isEqualTo(user.getId());
        assertThat(refreshed.refreshToken()).isEqualTo("second-token");
        assertThat(refreshed.refreshTokenExpiresAt())
                .isEqualTo(NOW.plusSeconds(60).plusSeconds(7L * 24 * 60 * 60));
        assertThat(refreshTokens.findByTokenHash(tokenHasher.hash("first-token")))
                .get()
                .extracting(SessionRefreshToken::getUsedAt)
                .isEqualTo(NOW.plusSeconds(60));
        assertThat(refreshTokens.findByTokenHash(tokenHasher.hash("second-token")))
                .get()
                .matches(token -> token.getUsedAt() == null);
    }

    @Test
    @Transactional
    void inactiveRefreshTokenExpiresAfterSevenDays() {
        AppUser user = createUser("inactive");
        when(tokenGenerator.generate()).thenReturn("inactive-token");
        IssuedSession issued = sessionService.create(user, null);

        when(clock.instant()).thenReturn(NOW.plusSeconds(7L * 24 * 60 * 60));

        assertThatThrownBy(() -> sessionService.refresh(issued.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @Transactional
    void activeRotationNeverExtendsSessionPastThirtyDays() {
        AppUser user = createUser("absolute");
        when(tokenGenerator.generate()).thenReturn(
                "day-zero-token",
                "day-six-token",
                "day-twelve-token",
                "day-eighteen-token",
                "day-twenty-four-token",
                "day-twenty-nine-token"
        );
        String currentToken = sessionService.create(user, null).refreshToken();

        for (int day : List.of(6, 12, 18, 24, 29)) {
            when(clock.instant()).thenReturn(NOW.plusSeconds(day * 24L * 60 * 60));
            RefreshedSession refreshed = sessionService.refresh(currentToken);
            currentToken = refreshed.refreshToken();

            if (day == 29) {
                assertThat(refreshed.refreshTokenExpiresAt())
                        .isEqualTo(NOW.plusSeconds(30L * 24 * 60 * 60));
            }
        }

        when(clock.instant()).thenReturn(NOW.plusSeconds(30L * 24 * 60 * 60));
        String finalToken = currentToken;
        assertThatThrownBy(() -> sessionService.refresh(finalToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @Transactional
    void replayRevokesOnlyTheCompromisedSessionFamily() {
        AppUser user = createUser("replay");
        when(tokenGenerator.generate()).thenReturn("replayed-token", "independent-token", "rotated-token");
        IssuedSession compromised = sessionService.create(user, "Browser One");
        IssuedSession independent = sessionService.create(user, "Browser Two");

        when(clock.instant()).thenReturn(NOW.plusSeconds(60));
        sessionService.refresh(compromised.refreshToken());

        when(clock.instant()).thenReturn(NOW.plusSeconds(120));
        assertThatThrownBy(() -> sessionService.refresh(compromised.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);

        AuthSession revoked = sessions.findForUpdateByRefreshTokenHash(
                tokenHasher.hash("rotated-token")
        ).orElseThrow();
        assertThat(revoked.getRevocationReason()).isEqualTo(SessionRevocationReason.REFRESH_TOKEN_REUSE);

        when(clock.instant()).thenReturn(NOW.plusSeconds(180));
        assertThatThrownBy(() -> sessionService.refresh("rotated-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        when(tokenGenerator.generate()).thenReturn("independent-rotated-token");
        assertThat(sessionService.refresh(independent.refreshToken()).refreshToken())
                .isEqualTo("independent-rotated-token");
    }

    @Test
    @Transactional
    void logoutIsIdempotentAndOnlyRevokesCurrentSession() {
        AppUser user = createUser("logout");
        when(tokenGenerator.generate()).thenReturn("logout-token", "other-token");
        IssuedSession current = sessionService.create(user, "Browser One");
        IssuedSession other = sessionService.create(user, "Browser Two");

        sessionService.revokeCurrent(current.refreshToken());
        sessionService.revokeCurrent(current.refreshToken());

        when(clock.instant()).thenReturn(NOW.plusSeconds(1));
        assertThatThrownBy(() -> sessionService.refresh(current.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        when(tokenGenerator.generate()).thenReturn("other-rotated-token");
        assertThat(sessionService.refresh(other.refreshToken()).refreshToken())
                .isEqualTo("other-rotated-token");
    }

    @Test
    @Transactional
    void revokeAllInvalidatesEveryActiveSessionForUser() {
        AppUser user = createUser("revoke-all");
        when(tokenGenerator.generate()).thenReturn("first-token", "second-token");
        IssuedSession first = sessionService.create(user, "Browser One");
        IssuedSession second = sessionService.create(user, "Browser Two");

        assertThat(sessionService.revokeAll(user.getId(), SessionRevocationReason.PASSWORD_RESET))
                .isEqualTo(2);

        assertThatThrownBy(() -> sessionService.refresh(first.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> sessionService.refresh(second.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @Transactional
    void cleanupDeletesOnlySessionsPastTheRetentionWindow() {
        AppUser user = createUser("cleanup");
        AuthSession oldExpired = sessions.save(new AuthSession(
                user,
                null,
                NOW.minusSeconds(70L * 24 * 60 * 60),
                NOW.minusSeconds(60L * 24 * 60 * 60)
        ));
        refreshTokens.save(new SessionRefreshToken(
                oldExpired,
                "a".repeat(64),
                oldExpired.getCreatedAt(),
                oldExpired.getExpiresAt()
        ));

        AuthSession recentlyExpired = sessions.save(new AuthSession(
                user,
                null,
                NOW.minusSeconds(35L * 24 * 60 * 60),
                NOW.minusSeconds(5L * 24 * 60 * 60)
        ));
        refreshTokens.save(new SessionRefreshToken(
                recentlyExpired,
                "b".repeat(64),
                recentlyExpired.getCreatedAt(),
                recentlyExpired.getExpiresAt()
        ));
        refreshTokens.flush();

        assertThat(sessionService.cleanupExpiredAndRevoked()).isEqualTo(1);
        assertThat(sessions.findById(oldExpired.getId())).isEmpty();
        assertThat(sessions.findById(recentlyExpired.getId())).isPresent();
    }

    @Test
    void concurrentRefreshOfSameTokenIssuesOnceThenRevokesTheReplayedFamily() throws Exception {
        AppUser user = createUser("concurrent");
        when(tokenGenerator.generate()).thenAnswer(invocation -> UUID.randomUUID().toString());
        IssuedSession issued = sessionService.create(user, "Concurrent Test Browser");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<RefreshOutcome> first = executor.submit(() -> refreshWhenReleased(issued.refreshToken(), ready, start));
            Future<RefreshOutcome> second = executor.submit(() -> refreshWhenReleased(issued.refreshToken(), ready, start));

            ready.await();
            start.countDown();
            List<RefreshOutcome> outcomes = List.of(first.get(), second.get());

            assertThat(outcomes).filteredOn(RefreshOutcome::succeeded).hasSize(1);
            assertThat(outcomes).filteredOn(outcome -> !outcome.succeeded()).hasSize(1);

            String rotatedToken = outcomes.stream()
                    .filter(RefreshOutcome::succeeded)
                    .map(RefreshOutcome::refreshToken)
                    .findFirst()
                    .orElseThrow();
            assertThatThrownBy(() -> sessionService.refresh(rotatedToken))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            String reason = jdbcTemplate.queryForObject(
                    "SELECT revocation_reason FROM auth_sessions WHERE user_id = ?",
                    String.class,
                    user.getId()
            );
            assertThat(reason).isEqualTo("REFRESH_TOKEN_REUSE");
        } finally {
            executor.shutdownNow();
        }
    }

    private RefreshOutcome refreshWhenReleased(
            String rawToken,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();

        try {
            RefreshedSession refreshed = sessionService.refresh(rawToken);
            return new RefreshOutcome(true, refreshed.refreshToken());
        } catch (InvalidRefreshTokenException exception) {
            return new RefreshOutcome(false, null);
        }
    }

    private AppUser createUser(String suffix) {
        return userService.createUser(
                "Session",
                "Tester",
                suffix + ".session@example.com",
                "hashed-password",
                "+1415555" + Math.abs(suffix.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private record RefreshOutcome(boolean succeeded, String refreshToken) {
    }
}
