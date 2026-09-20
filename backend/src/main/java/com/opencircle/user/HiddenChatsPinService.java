package com.opencircle.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class HiddenChatsPinService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final HiddenChatsPinProperties properties;
    private final Clock clock;

    HiddenChatsPinService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            HiddenChatsPinProperties properties,
            Clock clock
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    // Changing an existing PIN requires the account password, same rigor as changing the
    // password itself - it overwrites something already protecting hidden content. The very
    // first PIN a user sets needs no such confirmation: nothing is protected yet, so a hijacked
    // session couldn't do meaningful damage by setting one. Bean Validation on the request
    // already confirms the PIN is 4-6 digits.
    @Transactional
    public void setPin(UUID userId, String currentPassword, String rawPin) {
        AppUser user = userService.getByIdForUpdate(userId);

        boolean confirmationRequired = user.hasHiddenChatsPin();
        boolean confirmationFailed = currentPassword == null
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash());
        if (confirmationRequired && confirmationFailed) {
            throw new HiddenChatsPinConfirmationException();
        }

        user.setHiddenChatsPin(passwordEncoder.encode(rawPin));
    }

    // Verifies a PIN against an already-loaded current user, recording the outcome so
    // repeated wrong guesses eventually lock out further attempts for a cooldown period.
    @Transactional
    public void verifyPin(AppUser user, String rawPin) {
        Instant now = Instant.now(clock);

        if (!user.hasHiddenChatsPin()) {
            throw new HiddenChatsPinNotSetException();
        }

        if (user.isHiddenChatsPinLocked(now)) {
            throw new HiddenChatsPinLockedException();
        }

        if (rawPin == null || !passwordEncoder.matches(rawPin, user.getHiddenChatsPinHash())) {
            user.recordHiddenChatsPinFailure(now, properties.getMaxAttempts(), properties.getLockoutDuration());
            userService.save(user);
            throw new HiddenChatsPinIncorrectException();
        }

        user.recordHiddenChatsPinSuccess();
        userService.save(user);
    }
}
