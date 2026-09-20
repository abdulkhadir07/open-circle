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

    // Setting/changing the PIN requires the account password, same rigor as changing the
    // password itself. Bean Validation on the request already confirms the PIN is 4-6 digits.
    @Transactional
    public void setPin(UUID userId, String currentPassword, String rawPin) {
        AppUser user = userService.getByIdForUpdate(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
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
