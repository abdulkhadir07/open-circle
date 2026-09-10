package com.opencircle.profileimage;

import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_images")
class ProfileImage {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 120;
    private static final int MAX_BUCKET_LENGTH = 255;
    private static final int MAX_OBJECT_KEY_LENGTH = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "original_filename", nullable = false, length = MAX_FILENAME_LENGTH)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = MAX_CONTENT_TYPE_LENGTH)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "s3_bucket", nullable = false, length = MAX_BUCKET_LENGTH)
    private String s3Bucket;

    @Column(name = "s3_object_key", nullable = false, length = MAX_OBJECT_KEY_LENGTH, unique = true)
    private String s3ObjectKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProfileImage() {
    }

    ProfileImage(
            AppUser user,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String s3Bucket,
            String s3ObjectKey,
            Instant createdAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.user = user;
        this.createdAt = createdAt;
        replace(originalFilename, contentType, fileSizeBytes, s3Bucket, s3ObjectKey, createdAt);
    }

    void replace(
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String s3Bucket,
            String s3ObjectKey,
            Instant updatedAt
    ) {
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException("Updated time is required");
        }

        this.originalFilename = requiredText(
                originalFilename,
                MAX_FILENAME_LENGTH,
                "Original filename is required",
                "Original filename cannot exceed 255 characters"
        );
        this.contentType = requiredText(
                contentType,
                MAX_CONTENT_TYPE_LENGTH,
                "Content type is required",
                "Content type cannot exceed 120 characters"
        );
        this.fileSizeBytes = fileSizeBytes;
        this.s3Bucket = requiredText(
                s3Bucket,
                MAX_BUCKET_LENGTH,
                "S3 bucket is required",
                "S3 bucket cannot exceed 255 characters"
        );
        this.s3ObjectKey = requiredText(
                s3ObjectKey,
                MAX_OBJECT_KEY_LENGTH,
                "S3 object key is required",
                "S3 object key cannot exceed 1024 characters"
        );
        this.updatedAt = updatedAt;
    }

    private String requiredText(String value, int maxLength, String requiredMessage, String lengthMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(lengthMessage);
        }

        return normalized;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return user.getId();
    }

    String getOriginalFilename() {
        return originalFilename;
    }

    String getContentType() {
        return contentType;
    }

    long getFileSizeBytes() {
        return fileSizeBytes;
    }

    String getS3Bucket() {
        return s3Bucket;
    }

    String getS3ObjectKey() {
        return s3ObjectKey;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
