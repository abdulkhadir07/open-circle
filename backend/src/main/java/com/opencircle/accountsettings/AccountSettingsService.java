package com.opencircle.accountsettings;

import com.opencircle.common.OtpCodeGenerator;
import com.opencircle.mail.MailService;
import com.opencircle.passwordreset.PasswordResetService;
import com.opencircle.security.JwtService;
import com.opencircle.session.IssuedSession;
import com.opencircle.session.SessionRevocationReason;
import com.opencircle.session.SessionService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import com.opencircle.verification.EmailVerificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AccountSettingsService.class);

    private final UserService userService;
    private final EmailChangeRequestRepository emailChanges;
    private final EmailClaimLock emailClaimLock;
    private final PasswordResetService passwordResetService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final OtpCodeGenerator codeGenerator;
    private final EmailVerificationProperties verificationProperties;
    private final MailService mailService;
    private final JwtService jwtService;
    private final Clock clock;

    AccountSettingsService(
            UserService userService,
            EmailChangeRequestRepository emailChanges,
            EmailClaimLock emailClaimLock,
            PasswordResetService passwordResetService,
            SessionService sessionService,
            PasswordEncoder passwordEncoder,
            OtpCodeGenerator codeGenerator,
            EmailVerificationProperties verificationProperties,
            MailService mailService,
            JwtService jwtService,
            Clock clock
    ) {
        this.userService = userService;
        this.emailChanges = emailChanges;
        this.emailClaimLock = emailClaimLock;
        this.passwordResetService = passwordResetService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.codeGenerator = codeGenerator;
        this.verificationProperties = verificationProperties;
        this.mailService = mailService;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AccountSecurityUpdate changePassword(
            UUID userId,
            UUID currentSessionId,
            String currentPassword,
            String newPassword
    ) {
        AppUser user = userService.getByIdForUpdate(userId);
        verifyCurrentPassword(user, currentPassword);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new NewPasswordMatchesCurrentException();
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        passwordResetService.invalidateActiveCodes(user);
        IssuedSession session = sessionService.rotateCurrentAndRevokeOthers(
                userId,
                currentSessionId,
                SessionRevocationReason.PASSWORD_CHANGE
        );

        return securityUpdate(user, session);
    }

    @Transactional
    public void requestEmailChange(UUID userId, String newEmail, String currentPassword) {
        AppUser user = userService.getByIdForUpdate(userId);
        verifyCurrentPassword(user, currentPassword);

        String normalizedEmail = normalizeEmail(newEmail);
        if (normalizedEmail.equals(user.getEmail())) {
            throw new EmailUnchangedException();
        }
        if (userService.emailExists(normalizedEmail)) {
            throw new EmailAlreadyInUseException();
        }

        Instant now = Instant.now(clock);
        emailChanges.findByUserAndUsedAtIsNull(user)
                .ifPresent(request -> {
                    request.markUsed(now);
                    emailChanges.flush();
                });

        String rawCode = codeGenerator.generate();
        Instant expiresAt = now.plusSeconds(verificationProperties.getCodeExpirationMinutes() * 60L);
        emailChanges.save(new EmailChangeRequest(
                user,
                normalizedEmail,
                passwordEncoder.encode(rawCode),
                now,
                expiresAt
        ));
        mailService.sendEmailChangeCode(normalizedEmail, rawCode);
    }

    @Transactional(noRollbackFor = {
            EmailChangeCodeInvalidException.class,
            EmailChangeAttemptsExceededException.class,
            EmailAlreadyInUseException.class
    })
    public AccountSecurityUpdate verifyEmailChange(
            UUID userId,
            UUID currentSessionId,
            String rawCode
    ) {
        AppUser user = userService.getByIdForUpdate(userId);
        EmailChangeRequest request = emailChanges.findActiveForUpdate(userId)
                .orElseThrow(EmailChangeCodeInvalidException::new);
        Instant now = Instant.now(clock);
        int maxAttempts = verificationProperties.getMaxAttempts();

        if (request.getAttemptCount() >= maxAttempts) {
            throw new EmailChangeAttemptsExceededException();
        }
        if (!request.isActive(now, maxAttempts)) {
            throw new EmailChangeCodeInvalidException();
        }
        if (!passwordEncoder.matches(rawCode, request.getCodeHash())) {
            request.recordFailedAttempt();
            if (request.getAttemptCount() >= maxAttempts) {
                throw new EmailChangeAttemptsExceededException();
            }
            throw new EmailChangeCodeInvalidException();
        }

        emailClaimLock.acquire(request.getNewEmail());
        if (userService.emailExists(request.getNewEmail())) {
            request.markUsed(now);
            throw new EmailAlreadyInUseException();
        }

        String oldEmail = user.getEmail();
        request.markUsed(now);
        user.changeVerifiedEmail(request.getNewEmail(), now);

        userService.saveAndFlush(user);

        passwordResetService.invalidateActiveCodes(user);
        IssuedSession session = sessionService.rotateCurrentAndRevokeOthers(
                userId,
                currentSessionId,
                SessionRevocationReason.EMAIL_CHANGE
        );
        notifyOldEmailAfterCommit(oldEmail, user.getEmail());
        return securityUpdate(user, session);
    }

    private AccountSecurityUpdate securityUpdate(AppUser user, IssuedSession session) {
        return new AccountSecurityUpdate(
                jwtService.generateToken(user, session.sessionId()),
                session
        );
    }

    private void verifyCurrentPassword(AppUser user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void notifyOldEmailAfterCommit(String oldEmail, String newEmail) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    mailService.sendEmailChangedNotice(oldEmail, newEmail);
                } catch (RuntimeException exception) {
                    log.warn("Failed to send email-change security notice to {}", oldEmail, exception);
                }
            }
        });
    }
}
