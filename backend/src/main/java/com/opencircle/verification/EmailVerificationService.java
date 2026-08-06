package com.opencircle.verification;

import com.opencircle.common.OtpCodeGenerator;
import com.opencircle.mail.MailService;
import com.opencircle.user.AppUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class EmailVerificationService {

    private final EmailVerificationCodeRepository codes;
    private final OtpCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final EmailVerificationProperties properties;
    private final Clock clock;

    EmailVerificationService(
            EmailVerificationCodeRepository codes,
            OtpCodeGenerator codeGenerator,
            PasswordEncoder passwordEncoder,
            MailService mailService,
            EmailVerificationProperties properties,
            Clock clock
    ) {
        this.codes = codes;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void issueCode(AppUser user) {
        Instant now = Instant.now(clock);

        // Mark existing unused verification codes as used before creating a new one.
        codes.findByUserAndUsedAtIsNull(user)
                .forEach(code -> code.markUsed(now));

        // Generate a new verification code and store only its hashed value.
        String rawCode = codeGenerator.generate();
        String codeHash = passwordEncoder.encode(rawCode);
        Instant expiresAt = now.plusSeconds(properties.getCodeExpirationMinutes() * 60L);

        // Save the hashed verification code and email the raw code to the user.
        codes.save(new EmailVerificationCode(user, codeHash, expiresAt));
        mailService.sendEmailVerificationCode(user.getEmail(), rawCode);
    }

    @Transactional
    public void verify(AppUser user, String rawCode) {
        Instant now = Instant.now(clock);

        // Find the most recent unused verification code for this user.
        EmailVerificationCode code = codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(VerificationCodeInvalidException::new);

        // Stop email verification if the code has already reached the maximum number of attempts.
        if (code.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new VerificationAttemptsExceededException();
        }

        // Stop email verification if the code is already used, expired, or no longer active.
        if (!code.isActive(now, properties.getMaxAttempts())) {
            throw new VerificationCodeInvalidException();
        }

        // Compare the submitted code with the stored hashed verification code.
        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            // Record one failed attempt when the submitted code does not match.
            code.recordFailedAttempt();

            // Stop future guesses once the failed attempt reaches the maximum attempt limit.
            if (code.getAttemptCount() >= properties.getMaxAttempts()) {
                throw new VerificationAttemptsExceededException();
            }

            throw new VerificationCodeInvalidException();
        }

        // Mark the verification code as used after the submitted code matches.
        code.markUsed(now);

        // Mark the user's email as verified.
        user.markEmailVerified(now);
    }
}
