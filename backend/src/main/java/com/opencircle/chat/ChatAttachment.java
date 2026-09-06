package com.opencircle.chat;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_attachments")
class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_message_id", nullable = false, unique = true)
    private ChatMessage chatMessage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatAttachment() {
    }

    ChatAttachment(
            ChatMessage chatMessage,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String s3Bucket,
            String s3ObjectKey,
            Instant createdAt
    ) {
        if (chatMessage == null) {
            throw new IllegalArgumentException("Chat message is required");
        }

        if (!chatMessage.isAttachment()) {
            throw new IllegalArgumentException("Attachment metadata requires an attachment message");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.chatMessage = chatMessage;
        this.chatRoom = chatMessage.getChatRoom();
        this.uploader = chatMessage.getSender();
        this.originalFilename = requiredText(originalFilename, 255, "Original filename is required", "Original filename cannot exceed 255 characters");
        this.contentType = requiredText(contentType, 120, "Content type is required", "Content type cannot exceed 120 characters");
        this.fileSizeBytes = fileSizeBytes;
        this.s3Bucket = requiredText(s3Bucket, 255, "S3 bucket is required", "S3 bucket cannot exceed 255 characters");
        this.s3ObjectKey = requiredText(s3ObjectKey, 1024, "S3 object key is required", "S3 object key cannot exceed 1024 characters");
        this.createdAt = createdAt;
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

    ChatMessage getChatMessage() {
        return chatMessage;
    }

    ChatRoom getChatRoom() {
        return chatRoom;
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

    Instant getCreatedAt() {
        return createdAt;
    }
}