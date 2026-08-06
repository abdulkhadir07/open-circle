package com.opencircle.passwordreset;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetCodeTest {

    @Test
    void isActiveReturnsTrueWhenCodeIsUnusedAndNotExpired() {
        Instant now = Instant.now();
        PasswordResetCode code = new PasswordResetCode(
                user(),
                "hash",
                now.plusSeconds(60)
        );

        assertThat(code.isActive(now, 5)).isTrue();
    }

    @Test
    void isActiveReturnsFalseWhenCodeIsUsed() {
        Instant now = Instant.now();
        PasswordResetCode code = new PasswordResetCode(
                user(),
                "hash",
                now.plusSeconds(60)
        );

        code.markUsed(now);

        assertThat(code.isActive(now, 5)).isFalse();
    }

    @Test
    void isActiveReturnsFalseWhenCodeIsExpired() {
        Instant now = Instant.now();
        PasswordResetCode code = new PasswordResetCode(
                user(),
                "hash",
                now.minusSeconds(1)
        );

        assertThat(code.isActive(now, 5)).isFalse();
    }

    @Test
    void isActiveReturnsFalseWhenCodeReachedMaxAttempts() {
        Instant now = Instant.now();
        PasswordResetCode code = new PasswordResetCode(
                user(),
                "hash",
                now.plusSeconds(60)
        );

        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();

        assertThat(code.isActive(now, 3)).isFalse();
    }

    @Test
    void recordFailedAttemptIncrementsAttemptCount() {
        PasswordResetCode code = new PasswordResetCode(
                user(),
                "hash",
                Instant.now().plusSeconds(60)
        );

        code.recordFailedAttempt();

        assertThat(code.getAttemptCount()).isEqualTo(1);
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
