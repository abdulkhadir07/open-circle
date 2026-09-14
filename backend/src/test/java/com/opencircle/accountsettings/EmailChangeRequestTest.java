package com.opencircle.accountsettings;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailChangeRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-13T12:00:00Z");

    @Test
    void normalizesEmailAndStartsActive() {
        EmailChangeRequest request = request("  New.Email@Example.COM  ");

        assertThat(request.getNewEmail()).isEqualTo("new.email@example.com");
        assertThat(request.isActive(CREATED_AT.plusSeconds(60), 5)).isTrue();
    }

    @Test
    void expiresAtTheBoundary() {
        EmailChangeRequest request = request("new@example.com");

        assertThat(request.isActive(CREATED_AT.plusSeconds(15 * 60), 5)).isFalse();
    }

    @Test
    void failedAttemptsReachTheConfiguredLimit() {
        EmailChangeRequest request = request("new@example.com");

        for (int attempt = 0; attempt < 5; attempt++) {
            request.recordFailedAttempt();
        }

        assertThat(request.getAttemptCount()).isEqualTo(5);
        assertThat(request.isActive(CREATED_AT.plusSeconds(60), 5)).isFalse();
    }

    @Test
    void usedRequestCannotBeReactivated() {
        EmailChangeRequest request = request("new@example.com");
        request.markUsed(CREATED_AT.plusSeconds(30));

        request.markUsed(CREATED_AT.plusSeconds(60));

        assertThat(request.getUsedAt()).isEqualTo(CREATED_AT.plusSeconds(30));
        assertThat(request.isActive(CREATED_AT.plusSeconds(90), 5)).isFalse();
    }

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new EmailChangeRequest(
                user(),
                "new@example.com",
                "hashed-code",
                CREATED_AT,
                CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private EmailChangeRequest request(String email) {
        return new EmailChangeRequest(
                user(),
                email,
                "hashed-code",
                CREATED_AT,
                CREATED_AT.plusSeconds(15 * 60)
        );
    }

    private AppUser user() {
        return new AppUser(
                "settings_user_1234",
                "Settings",
                "Tester",
                "settings@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
