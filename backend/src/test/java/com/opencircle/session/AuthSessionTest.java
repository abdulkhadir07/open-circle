package com.opencircle.session;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthSessionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-13T12:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-13T12:00:00Z");

    @Test
    void createsActiveSessionAndNormalizesUserAgent() {
        AuthSession session = new AuthSession(user(), "  OpenCircle Test Browser  ", CREATED_AT, EXPIRES_AT);

        assertThat(session.isActive(CREATED_AT)).isTrue();
        assertThat(session.getUserAgent()).isEqualTo("OpenCircle Test Browser");
        assertThat(session.getLastUsedAt()).isEqualTo(CREATED_AT);
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    void truncatesUntrustedUserAgentMetadataToDatabaseLimit() {
        AuthSession session = new AuthSession(user(), "x".repeat(600), CREATED_AT, EXPIRES_AT);

        assertThat(session.getUserAgent()).hasSize(AuthSession.MAX_USER_AGENT_LENGTH);
    }

    @Test
    void revocationIsIdempotentAndPreservesTheFirstReason() {
        AuthSession session = new AuthSession(user(), null, CREATED_AT, EXPIRES_AT);

        session.revoke(CREATED_AT.plusSeconds(10), SessionRevocationReason.LOGOUT);
        session.revoke(CREATED_AT.plusSeconds(20), SessionRevocationReason.REFRESH_TOKEN_REUSE);

        assertThat(session.isActive(CREATED_AT.plusSeconds(30))).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(CREATED_AT.plusSeconds(10));
        assertThat(session.getRevocationReason()).isEqualTo(SessionRevocationReason.LOGOUT);
    }

    @Test
    void rejectsUseAfterRevocation() {
        AuthSession session = new AuthSession(user(), null, CREATED_AT, EXPIRES_AT);
        session.revoke(CREATED_AT.plusSeconds(1), SessionRevocationReason.LOGOUT);

        assertThatThrownBy(() -> session.recordUse(CREATED_AT.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Revoked session cannot be used");
    }

    @Test
    void refreshTokenCanOnlyBeConsumedOnce() {
        AuthSession session = new AuthSession(user(), null, CREATED_AT, EXPIRES_AT);
        SessionRefreshToken token = new SessionRefreshToken(
                session,
                "a".repeat(64),
                CREATED_AT,
                EXPIRES_AT
        );

        token.markUsed(CREATED_AT.plusSeconds(1));

        assertThat(token.isActive(CREATED_AT.plusSeconds(2))).isFalse();
        assertThatThrownBy(() -> token.markUsed(CREATED_AT.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Refresh token has already been used");
    }

    private AppUser user() {
        return new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
