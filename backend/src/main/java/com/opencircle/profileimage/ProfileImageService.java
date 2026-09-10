package com.opencircle.profileimage;

import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
class ProfileImageService {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageService.class);
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 120;

    private final ProfileImageRepository images;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final EntityManager entityManager;
    private final Clock clock;

    ProfileImageService(
            ProfileImageRepository images,
            StorageService storageService,
            StorageProperties storageProperties,
            EntityManager entityManager,
            Clock clock
    ) {
        this.images = images;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    ProfileImageResponse uploadOrReplace(AppUser user, ProfileImageUpload upload) {
        validate(upload);
        UUID userId = requireUserId(user);

        StoredFile storedFile = storageService.upload(
                storageKeyPrefix(userId),
                upload.inputStream(),
                upload.fileSizeBytes(),
                normalizedContentType(upload.contentType())
        );
        boolean cleanupRegistered = false;

        try {
            AppUser managedUser = lockUser(userId);
            ProfileImage image = images.findByUser_Id(userId).orElse(null);
            StoredObject previousObject = image == null ? null : StoredObject.from(image);
            Instant now = Instant.now(clock);

            if (image == null) {
                image = new ProfileImage(
                        managedUser,
                        upload.originalFilename(),
                        storedFile.contentType(),
                        storedFile.fileSizeBytes(),
                        storedFile.bucket(),
                        storedFile.key(),
                        now
                );
            } else {
                image.replace(
                        upload.originalFilename(),
                        storedFile.contentType(),
                        storedFile.fileSizeBytes(),
                        storedFile.bucket(),
                        storedFile.key(),
                        now
                );
            }

            ProfileImage savedImage = images.saveAndFlush(image);
            registerUploadCleanup(storedFile, previousObject);
            cleanupRegistered = true;

            return responseFor(savedImage);
        } catch (RuntimeException exception) {
            if (!cleanupRegistered) {
                deleteQuietly(storedFile.bucket(), storedFile.key());
            }

            throw exception;
        }
    }

    @Transactional
    void delete(AppUser user) {
        UUID userId = requireUserId(user);
        lockUser(userId);

        images.findByUser_Id(userId).ifPresent(image -> {
            StoredObject storedObject = StoredObject.from(image);
            images.delete(image);
            images.flush();
            registerDeleteAfterCommit(storedObject);
        });
    }

    private AppUser lockUser(UUID userId) {
        AppUser user = entityManager.find(AppUser.class, userId, LockModeType.PESSIMISTIC_WRITE);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return user;
    }

    private UUID requireUserId(AppUser user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Persisted user is required");
        }

        return user.getId();
    }

    private ProfileImageResponse responseFor(ProfileImage image) {
        return ProfileImageResponse.from(
                image,
                storageService.generateViewUrl(image.getS3Bucket(), image.getS3ObjectKey())
        );
    }

    private void validate(ProfileImageUpload upload) {
        if (upload == null || upload.inputStream() == null) {
            throw new InvalidProfileImageException("Image file is required");
        }

        if (upload.originalFilename() == null || upload.originalFilename().isBlank()) {
            throw new InvalidProfileImageException("Original filename is required");
        }

        if (upload.originalFilename().trim().length() > MAX_FILENAME_LENGTH) {
            throw new InvalidProfileImageException("Original filename is too long");
        }

        if (upload.contentType() == null || upload.contentType().isBlank()) {
            throw new InvalidProfileImageException("Content type is required");
        }

        if (upload.contentType().trim().length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new InvalidProfileImageException("Content type is too long");
        }

        if (upload.fileSizeBytes() <= 0) {
            throw new InvalidProfileImageException("File size must be greater than zero");
        }

        if (upload.fileSizeBytes() > storageProperties.getProfileImages().getMaxFileSizeBytes()) {
            throw new InvalidProfileImageException("File size exceeds the maximum allowed profile image size");
        }

        if (!storageProperties.isAllowedProfileImageContentType(upload.contentType())) {
            throw new InvalidProfileImageException("File type is not supported");
        }
    }

    private void registerUploadCleanup(StoredFile uploadedObject, StoredObject previousObject) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    if (previousObject != null) {
                        deleteQuietly(previousObject.bucket(), previousObject.key());
                    }
                } else {
                    deleteQuietly(uploadedObject.bucket(), uploadedObject.key());
                }
            }
        });
    }

    private void registerDeleteAfterCommit(StoredObject storedObject) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(storedObject.bucket(), storedObject.key());
            }
        });
    }

    private void deleteQuietly(String bucket, String key) {
        try {
            storageService.delete(bucket, key);
        } catch (RuntimeException exception) {
            log.warn("Unable to clean up stored profile image {}/{}", bucket, key, exception);
        }
    }

    private String storageKeyPrefix(UUID userId) {
        return "profile-images/users/" + userId;
    }

    private String normalizedContentType(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private record StoredObject(String bucket, String key) {

        private static StoredObject from(ProfileImage image) {
            return new StoredObject(image.getS3Bucket(), image.getS3ObjectKey());
        }
    }
}
