package com.opencircle.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HiddenChatsPinServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-19T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final UserService userService = mock(UserService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final HiddenChatsPinProperties properties = new HiddenChatsPinProperties();

    private final HiddenChatsPinService service =
            new HiddenChatsPinService(userService, passwordEncoder, properties, CLOCK);

    @Test
    void setPinEncodesAndStoresPinWhenCurrentPasswordMatches() {
        AppUser user = user();
        UUID userId = UUID.randomUUID();
        when(userService.getByIdForUpdate(userId)).thenReturn(user);
        when(passwordEncoder.matches("correct-password", user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("1234")).thenReturn("hashed-1234");

        service.setPin(userId, "correct-password", "1234");

        assertThat(user.getHiddenChatsPinHash()).isEqualTo("hashed-1234");
    }

    @Test
    void setPinRejectsIncorrectCurrentPassword() {
        AppUser user = user();
        UUID userId = UUID.randomUUID();
        when(userService.getByIdForUpdate(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.setPin(userId, "wrong-password", "1234"))
                .isInstanceOf(HiddenChatsPinConfirmationException.class);

        assertThat(user.hasHiddenChatsPin()).isFalse();
    }

    @Test
    void verifyPinRejectsWhenNoPinIsSet() {
        AppUser user = user();

        assertThatThrownBy(() -> service.verifyPin(user, "1234"))
                .isInstanceOf(HiddenChatsPinNotSetException.class);
    }

    @Test
    void verifyPinSucceedsAndResetsStateOnCorrectPin() {
        AppUser user = user();
        user.setHiddenChatsPin("hashed-1234");
        when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        service.verifyPin(user, "1234");

        assertThat(user.isHiddenChatsPinLocked(NOW)).isFalse();
        verify(userService).save(user);
    }

    @Test
    void verifyPinRecordsFailureAndThrowsOnIncorrectPin() {
        AppUser user = user();
        user.setHiddenChatsPin("hashed-1234");
        when(passwordEncoder.matches("0000", "hashed-1234")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyPin(user, "0000"))
                .isInstanceOf(HiddenChatsPinIncorrectException.class);

        verify(userService).save(user);
    }

    @Test
    void verifyPinLocksOutAfterMaxAttemptsAndRejectsFurtherAttemptsWithoutCheckingThePin() {
        AppUser user = user();
        user.setHiddenChatsPin("hashed-1234");
        properties.setMaxAttempts(2);
        when(passwordEncoder.matches("0000", "hashed-1234")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyPin(user, "0000"))
                .isInstanceOf(HiddenChatsPinIncorrectException.class);
        assertThatThrownBy(() -> service.verifyPin(user, "0000"))
                .isInstanceOf(HiddenChatsPinIncorrectException.class);

        assertThat(user.isHiddenChatsPinLocked(NOW)).isTrue();

        assertThatThrownBy(() -> service.verifyPin(user, "1234"))
                .isInstanceOf(HiddenChatsPinLockedException.class);
        verify(passwordEncoder, never()).matches("1234", "hashed-1234");
    }

    private AppUser user() {
        return new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "account-password-hash",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
