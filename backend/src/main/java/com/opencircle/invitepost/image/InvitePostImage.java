package com.opencircle.invitepost.image;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_post_images")
class InvitePostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_post_id", nullable = false)
    private InvitePost invitePost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private AppUser uploader;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "s3_bucket", nullable = false, length = 255)
    private String s3Bucket;

    @Column(name = "s3_object_key", nullable = false, length = 1024, unique = true)
    private String s3ObjectKey;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InvitePostImage() {
    }

    InvitePostImage(
            InvitePost invitePost,
            AppUser uploader,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String s3Bucket,
            String s3ObjectKey,
            int displayOrder,
            Instant createdAt
    ) {
        if (invitePost == null) {
            throw new IllegalArgumentException("Invite post is required");
        }

        if (uploader == null) {
            throw new IllegalArgumentException("Uploader is required");
        }

        if (!sameUser(invitePost.getPoster(), uploader)) {
            throw new IllegalArgumentException("Only the invite post poster can upload images");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }

        if (displayOrder < 1 || displayOrder > 4) {
            throw new IllegalArgumentException("Display order must be between 1 and 4");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.invitePost = invitePost;
        this.uploader = uploader;
        this.originalFilename = requiredText(originalFilename, 255, "Original filename is required", "Original filename cannot exceed 255 characters");
        this.contentType = requiredText(contentType, 120, "Content type is required", "Content type cannot exceed 120 characters");
        this.fileSizeBytes = fileSizeBytes;
        this.s3Bucket = requiredText(s3Bucket, 255, "S3 bucket is required", "S3 bucket cannot exceed 255 characters");
        this.s3ObjectKey = requiredText(s3ObjectKey, 1024, "S3 object key is required", "S3 object key cannot exceed 1024 characters");
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first.getId() != null && first.getId().equals(second.getId());
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

    InvitePost getInvitePost() {
        return invitePost;
    }

    AppUser getUploader() {
        return uploader;
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

    int getDisplayOrder() {
        return displayOrder;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}