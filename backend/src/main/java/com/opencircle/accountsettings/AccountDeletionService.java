package com.opencircle.accountsettings;

import com.opencircle.storage.StorageService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class AccountDeletionService {

    private final UserService userService;
    private final AccountDeletionRepository accountDeletionRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    AccountDeletionService(
            UserService userService,
            AccountDeletionRepository accountDeletionRepository,
            StorageService storageService,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.accountDeletionRepository = accountDeletionRepository;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    void deleteAccount(UUID userId, String currentPassword, String confirmation) {
        if (!"DELETE".equals(confirmation)) {
            throw new AccountDeletionConfirmationException();
        }

        AppUser user = userService.getByIdForUpdate(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }

        for (StoredAccountObject storedObject : accountDeletionRepository.findStoredObjects(userId)) {
            storageService.delete(storedObject.bucket(), storedObject.key());
        }

        accountDeletionRepository.deleteNotificationsRelatedTo(userId);
        userService.deleteAndFlush(user);
    }
}
