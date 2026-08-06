package com.opencircle.verification;

import com.opencircle.mail.MailService;
import com.opencircle.user.AppUser;
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

class EmailVerificationServiceTest {

    private final EmailVerificationCodeRepository codes = mock(EmailVerificationCodeRepository.class);
    private final VerificationCodeGenerator codeGenerator = mock(VerificationCodeGenerator.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final MailService mailService = mock(MailService.class);
    private final EmailVerificationProperties properties = properties();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), ZoneOffset.UTC);

    private final EmailVerificationService service = new EmailVerificationService(
            codes,
            codeGenerator,
            passwordEncoder,
            mailService,
            properties,
            clock
    );

    @Test
    void issueCodeInvalidatesExistingUnusedCodesBeforeSendingNewCode() {
        AppUser user = user();
        EmailVerificationCode oldCode = new EmailVerificationCode(
                user,
                "old-hash",
                Instant.parse("2026-08-05T00:10:00Z")
        );

        when(codes.findByUserAndUsedAtIsNull(user)).thenReturn(List.of(oldCode));
        when(codeGenerator.generate()).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("new-hash");

        service.issueCode(user);

        assertThat(oldCode.isUsed()).isTrue();
        assertThat(oldCode.getUsedAt()).isEqualTo(Instant.parse("2026-08-05T00:00:00Z"));

        var captor = forClass(EmailVerificationCode.class);
        verify(codes).save(captor.capture());

        EmailVerificationCode savedCode = captor.getValue();
        assertThat(savedCode.getUser()).isEqualTo(user);
        assertThat(savedCode.getCodeHash()).isEqualTo("new-hash");
        assertThat(savedCode.getExpiresAt()).isEqualTo(Instant.parse("2026-08-05T00:15:00Z"));

        verify(mailService).sendEmailVerificationCode("jane@example.com", "123456");
    }

    @Test
    void verifyMarksUserVerifiedWhenCodeMatches() {
        AppUser user = user();
        EmailVerificationCode code = new EmailVerificationCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-05T00:15:00Z")
        );

        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);

        service.verify(user, "123456");

        assertThat(code.isUsed()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(Instant.parse("2026-08-05T00:00:00Z"));
    }

    @Test
    void verifyRecordsAttemptAndRejectsWrongCode() {
        AppUser user = user();
        EmailVerificationCode code = new EmailVerificationCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-05T00:15:00Z")
        );

        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));
        when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.verify(user, "000000"))
                .isInstanceOf(VerificationCodeInvalidException.class)
                .hasMessage("Invalid or expired verification code");

        assertThat(code.getAttemptCount()).isEqualTo(1);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyRejectsCodeAfterMaxAttempts() {
        AppUser user = user();
        EmailVerificationCode code = new EmailVerificationCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-05T00:15:00Z")
        );

        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();
        code.recordFailedAttempt();

        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.verify(user, "123456"))
                .isInstanceOf(VerificationAttemptsExceededException.class)
                .hasMessage("Verification attempts exceeded. Please request a new code");
    }

    @Test
    void verifyThrowsInvalidWhenNoCodeExists() {
        AppUser user = user();

        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(user, "123456"))
                .isInstanceOf(VerificationCodeInvalidException.class)
                .hasMessage("Invalid or expired verification code");
    }

    @Test
    void verifyThrowsInvalidWhenCodeExpired() {
        AppUser user = user();
        EmailVerificationCode code = new EmailVerificationCode(
                user,
                "stored-hash",
                Instant.parse("2026-08-04T23:59:59Z")
        );

        when(codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.verify(user, "123456"))
                .isInstanceOf(VerificationCodeInvalidException.class)
                .hasMessage("Invalid or expired verification code");
    }

    private EmailVerificationProperties properties() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
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
