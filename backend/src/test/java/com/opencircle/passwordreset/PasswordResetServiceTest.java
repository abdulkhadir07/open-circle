package com.opencircle.passwordreset;

import com.opencircle.common.OtpCodeGenerator;
import com.opencircle.mail.MailService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private final PasswordResetCodeRepository codes = mock(PasswordResetCodeRepository.class);
    private final UserService userService = mock(UserService.class);
    private final OtpCodeGenerator codeGenerator = mock(OtpCodeGenerator.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final MailService mailService = mock(MailService.class);
    private final PasswordResetProperties properties = properties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-06T00:00:00Z"), ZoneOffset.UTC);

    private final PasswordResetService service = new PasswordResetService(
            codes,
            userService,
            codeGenerator,
            passwordEncoder,
            mailService,
            properties,
            clock
    );

    @Test
    void requestResetDoesNothingWhenEmailDoesNotExist() {
        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.requestReset("missing@example.com");

        verifyNoInteractions(codeGenerator, passwordEncoder, mailService);
        verify(codes, never()).save(any());
    }

    @Test
    void requestResetInvalidatesOldCodesAndSendsNewCodeWhenUserExists() {
        AppUser user = user();
        PasswordResetCode oldCode = new PasswordResetCode(
                user,
                "old-hash",
                Instant.parse("2026-08-06T00:10:00Z")
        );

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(oldCode));
        when(codeGenerator.generate()).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("new-hash");

        service.requestReset("jane@example.com");

        assertThat(oldCode.isUsed()).isTrue();
        assertThat(oldCode.getUsedAt()).isEqualTo(Instant.parse("2026-08-06T00:00:00Z"));

        var captor = forClass(PasswordResetCode.class);
        verify(codes).save(captor.capture());

        PasswordResetCode savedCode = captor.getValue();
        assertThat(savedCode.getUser()).isEqualTo(user);
        assertThat(savedCode.getCodeHash()).isEqualTo("new-hash");
        assertThat(savedCode.getExpiresAt()).isEqualTo(Instant.parse("2026-08-06T00:15:00Z"));

        verify(mailService).sendPasswordResetCode("jane@example.com", "123456");
    }

    @Test
    void resetPasswordChangesPasswordWhenCodeMatches() {
        AppUser user = user();
        PasswordResetCode code = new PasswordResetCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-06T00:15:00Z")
        );

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-password-hash");

        service.resetPassword("jane@example.com", "123456", "NewPassword123!");

        assertThat(code.isUsed()).isTrue();
        assertThat(user.getPasswordHash()).isEqualTo("new-password-hash");
    }

    @Test
    void resetPasswordThrowsInvalidWhenNoCodeExists() {
        AppUser user = user();

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("jane@example.com", "123456", "NewPassword123!"))
                .isInstanceOf(PasswordResetCodeInvalidException.class)
                .hasMessage("Invalid or expired password reset code");
    }

    @Test
    void resetPasswordThrowsInvalidWhenCodeExpired() {
        AppUser user = user();
        PasswordResetCode code = new PasswordResetCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-05T23:59:59Z")
        );

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.resetPassword("jane@example.com", "123456", "NewPassword123!"))
                .isInstanceOf(PasswordResetCodeInvalidException.class)
                .hasMessage("Invalid or expired password reset code");
    }

    @Test
    void resetPasswordRecordsAttemptAndRejectsWrongCode() {
        AppUser user = user();
        PasswordResetCode code = new PasswordResetCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-06T00:15:00Z")
        );

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));
        when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.resetPassword("jane@example.com", "000000", "NewPassword123!"))
                .isInstanceOf(PasswordResetCodeInvalidException.class)
                .hasMessage("Invalid or expired password reset code");

        assertThat(code.getAttemptCount()).isEqualTo(1);
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void resetPasswordRejectsCodeAfterMaxAttempts() {
        AppUser user = user();
        PasswordResetCode code = new PasswordResetCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-06T00:15:00Z")
        );

        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();

        when(userService.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.resetPassword("jane@example.com", "123456", "NewPassword123!"))
                .isInstanceOf(PasswordResetAttemptsExceededException.class)
                .hasMessage("Password reset attempts exceeded. Please request a new code");
    }

    private PasswordResetProperties properties() {
        PasswordResetProperties properties = new PasswordResetProperties();
        properties.setCodeExpirationMinutes(15);
        properties.setMaxAttempts(5);
        return properties;
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
