package com.opencircle.verification;

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
    private final VerificationCodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final EmailVerificationProperties properties;
    private final Clock clock;

    EmailVerificationService(
            EmailVerificationCodeRepository codes,
            VerificationCodeGenerator codeGenerator,
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

        // Only one verification code should be usable at a time. Old unused codes are invalidated first.
        codes.findByUserAndUsedAtIsNull(user)
                .forEach(code -> code.markUsed(now));

        String rawCode = codeGenerator.generate();
        String codeHash = passwordEncoder.encode(rawCode);
        Instant expiresAt = now.plusSeconds(properties.getCodeExpirationMinutes() * 60L);

        codes.save(new EmailVerificationCode(user, codeHash, expiresAt));
        mailService.sendEmailVerificationCode(user.getEmail(), rawCode);
    }

    @Transactional
    public void verify(AppUser user, String rawCode) {
        Instant now = Instant.now(clock);

        EmailVerificationCode code = codes.findFirstByUserAndUsedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(VerificationCodeInvalidException::new);

        if (code.getAttemptCount() >= properties.getMaxAttempts()) {
            throw new VerificationAttemptsExceededException();
        }

        if (!code.isActive(now, properties.getMaxAttempts())) {
            throw new VerificationCodeInvalidException();
        }

        if (!passwordEncoder.matches(rawCode, code.getCodeHash())) {
            code.recordFailedAttempt();

            if (code.getAttemptCount() >= properties.getMaxAttempts()) {
                throw new VerificationAttemptsExceededException();
            }

            throw new VerificationCodeInvalidException();
        }

        code.markUsed(now);
        user.markEmailVerified(now);
    }
}
