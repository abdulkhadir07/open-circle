package com.opencircle.profileimage;

import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileImageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

    private final ProfileImageRepository images = mock(ProfileImageRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final StorageProperties storageProperties = storageProperties();
    private final ProfileImageService service = new ProfileImageService(
            images,
            storageService,
            storageProperties,
            entityManager,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @BeforeEach
    void startTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void replacementDeletesPreviousObjectOnlyAfterCommit() {
        UUID userId = UUID.randomUUID();
        AppUser user = user(userId);
        ProfileImage previousImage = image(user, "old-key");
        StoredFile uploadedFile = new StoredFile("profile-bucket", "new-key", "image/webp", 11L);
        stubSuccessfulUpload(user, uploadedFile, Optional.of(previousImage));

        service.uploadOrReplace(user, upload("new.webp", "image/webp", 11L));

        verify(storageService, never()).delete(any(), any());

        completeSynchronization(TransactionSynchronization.STATUS_COMMITTED);

        verify(storageService).delete("profile-bucket", "old-key");
        verify(storageService, never()).delete("profile-bucket", "new-key");
    }

    @Test
    void replacementRollbackKeepsPreviousObjectAndDeletesNewObject() {
        UUID userId = UUID.randomUUID();
        AppUser user = user(userId);
        ProfileImage previousImage = image(user, "old-key");
        StoredFile uploadedFile = new StoredFile("profile-bucket", "new-key", "image/png", 7L);
        stubSuccessfulUpload(user, uploadedFile, Optional.of(previousImage));

        service.uploadOrReplace(user, upload("new.png", "image/png", 7L));

        completeSynchronization(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storageService).delete("profile-bucket", "new-key");
        verify(storageService, never()).delete("profile-bucket", "old-key");
    }

    @Test
    void databaseFailureDeletesNewlyUploadedObjectImmediately() {
        UUID userId = UUID.randomUUID();
        AppUser user = user(userId);
        StoredFile uploadedFile = new StoredFile("profile-bucket", "new-key", "image/png", 7L);

        when(storageService.upload(any(), any(), any(Long.class), any()))
                .thenReturn(uploadedFile);
        when(entityManager.find(AppUser.class, userId, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(images.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(images.saveAndFlush(any(ProfileImage.class))).thenThrow(new IllegalStateException("Database unavailable"));

        assertThatThrownBy(() -> service.uploadOrReplace(user, upload("new.png", "image/png", 7L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database unavailable");

        verify(storageService).delete("profile-bucket", "new-key");
    }

    @Test
    void validationRejectsUnsupportedTypeBeforeStorageAccess() {
        AppUser user = user(UUID.randomUUID());

        assertThatThrownBy(() -> service.uploadOrReplace(user, upload("avatar.gif", "image/gif", 7L)))
                .isInstanceOf(InvalidProfileImageException.class)
                .hasMessage("File type is not supported");

        verifyNoInteractions(storageService, images, entityManager);
    }

    private void stubSuccessfulUpload(
            AppUser user,
            StoredFile uploadedFile,
            Optional<ProfileImage> previousImage
    ) {
        when(storageService.upload(any(), any(), any(Long.class), any())).thenReturn(uploadedFile);
        when(entityManager.find(AppUser.class, user.getId(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(images.findByUser_Id(user.getId())).thenReturn(previousImage);
        when(images.saveAndFlush(any(ProfileImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.generateViewUrl(uploadedFile.bucket(), uploadedFile.key()))
                .thenReturn(new StorageAccessUrl(
                        URI.create("https://example.com/profile"),
                        NOW.plusSeconds(3600)
                ));
    }

    private void completeSynchronization(int status) {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
    }

    private ProfileImage image(AppUser user, String objectKey) {
        return new ProfileImage(
                user,
                "old.png",
                "image/png",
                7L,
                "profile-bucket",
                objectKey,
                NOW.minusSeconds(60)
        );
    }

    private AppUser user(UUID userId) {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }

    private ProfileImageUpload upload(String filename, String contentType, long size) {
        return new ProfileImageUpload(
                filename,
                contentType,
                size,
                new ByteArrayInputStream(new byte[Math.toIntExact(size)])
        );
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.getProfileImages().setMaxFileSizeBytes(5_242_880L);
        properties.getProfileImages().setAllowedContentTypes(
                List.of("image/jpeg", "image/png", "image/webp")
        );
        properties.getProfileImages().setViewUrlExpirationMinutes(60);
        return properties;
    }
}
