package com.opencircle.accountsettings;

import com.opencircle.storage.StorageService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountDeletionServiceTest {

    private final UserService userService = mock(UserService.class);
    private final AccountDeletionRepository repository = mock(AccountDeletionRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AccountDeletionService service = new AccountDeletionService(
            userService,
            repository,
            storageService,
            passwordEncoder
    );

    @Test
    void deletesStoredObjectsAndTheAccountAfterPasswordVerification() {
        UUID userId = UUID.randomUUID();
        AppUser user = mock(AppUser.class);
        when(user.getPasswordHash()).thenReturn("password-hash");
        when(userService.getByIdForUpdate(userId)).thenReturn(user);
        when(passwordEncoder.matches("Password123!", "password-hash")).thenReturn(true);
        when(repository.findStoredObjects(userId)).thenReturn(List.of(
                new StoredAccountObject("bucket", "profile-image"),
                new StoredAccountObject("bucket", "chat-attachment")
        ));

        service.deleteAccount(userId, "Password123!", "DELETE");

        var order = inOrder(storageService, repository, userService);
        order.verify(storageService).delete("bucket", "profile-image");
        order.verify(storageService).delete("bucket", "chat-attachment");
        order.verify(repository).deleteNotificationsRelatedTo(userId);
        order.verify(userService).deleteAndFlush(user);
    }

    @Test
    void rejectsAnIncorrectPasswordWithoutDeletingAnything() {
        UUID userId = UUID.randomUUID();
        AppUser user = mock(AppUser.class);
        when(user.getPasswordHash()).thenReturn("password-hash");
        when(userService.getByIdForUpdate(userId)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "password-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAccount(userId, "wrong", "DELETE"))
                .isInstanceOf(CurrentPasswordIncorrectException.class)
                .hasMessage("Current password is incorrect");

        verify(repository, never()).findStoredObjects(userId);
        verify(repository, never()).deleteNotificationsRelatedTo(userId);
        verify(userService, never()).deleteAndFlush(user);
    }

    @Test
    void requiresTheExactDeletionConfirmation() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.deleteAccount(userId, "Password123!", "delete"))
                .isInstanceOf(AccountDeletionConfirmationException.class)
                .hasMessage("Type DELETE to confirm account deletion");

        verify(userService, never()).getByIdForUpdate(userId);
    }
}
