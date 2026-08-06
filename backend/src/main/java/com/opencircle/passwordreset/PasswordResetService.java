package com.opencircle.passwordreset;

import com.opencircle.common.OtpCodeGenerator;
import com.opencircle.mail.MailService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PasswordResetService {

    private final PasswordResetCodeRepository codes;
    private final UserService userService;
    private final OtpCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordResetProperties properties;
    private final Clock clock;

    PasswordResetService(
            PasswordResetCodeRepository codes,
            UserService userService,
            OtpCodeGenerator codeGenerator,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            PasswordResetProperties properties,
            Clock clock
    ) {
        this.codes = codes;
        this.userService = userService;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void requestReset(String email) {
        // Send a reset code only when the email belongs to an existing user.
        userService.findByEmail(email).ifPresent(this::issueCode);
    }

    @Transactional
    public void resetPassword(String email, String rawCode, String newPassword) {
        // Find the user account for the submitted email address.
        AppUser user = userService.findByEmail(email)
                .orElseThrow(PasswordResetCodeInvalidException::new);

        Instant now = Instant.now(clock);

        // Find the most recent unused reset code for this user.
        PasswordResetCode code = codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(PasswordResetCodeInvalidException::new);

        // Stop password reset if the code has already reached the maximum number of attempts.
        if (code.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new PasswordResetAttemptsExceededException();
        }

        // Stop password reset if the code is already used, expired, or no longer active.
        if (!code.isActive(now, properties.getMaxAttempts())) {
            throw new PasswordResetCodeInvalidException();
        }

        // Compare the submitted code with the stored hashed reset code.
        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            // Record one failed attempt when the submitted code does not match.
            code.recordFailedAttempt();

            // Stop future guesses once the failed attempt reaches the maximum attempt limit.
            if (code.getAttemptCount() >= properties.getMaxAttempts()) {
                throw new PasswordResetAttemptsExceededException();
            }

            throw new PasswordResetCodeInvalidException();
        }

        // Mark the reset code as used after the submitted code matches.
        code.markUsed(now);

        // Hash and save the new password for the user.
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private void issueCode(AppUser user) {
        Instant now = Instant.now(clock);

        // Mark existing unused reset codes as used before creating a new one.
        codes.findByUserAndUsedAtIsNull(user)
                .forEach(code -> code.markUsed(now));

        // Generate a new reset code and store only its hashed value.
        String rawCode = codeGenerator.generate();
        String codeHash = passwordEncoder.encode(rawCode);
        Instant expiresAt = now.plusSeconds(properties.getCodeExpirationMinutes() * 60L);

        // Save the hashed reset code and email the raw code to the user.
        codes.save(new PasswordResetCode(user, codeHash, expiresAt));
        mailService.sendPasswordResetCode(user.getEmail(), rawCode);
    }
}
