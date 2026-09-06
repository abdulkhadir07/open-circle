package com.opencircle.chat;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    private static final int MAX_BODY_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatMessageType type;

    @Column(length = MAX_BODY_LENGTH)
    private String body;

    @OneToOne(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ChatAttachment attachment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    static ChatMessage text(ChatRoom chatRoom, AppUser sender, String body, Instant createdAt) {
        return new ChatMessage(chatRoom, sender, ChatMessageType.TEXT, requiredBody(body), createdAt);
    }

    // Attachment messages store the optional caption in body and the file metadata in chat_attachments.
    static ChatMessage attachment(
            ChatRoom chatRoom,
            AppUser sender,
            String caption,
            String originalFilename,
            String contentType,
            long fileSizeBytes,
            String s3Bucket,
            String s3ObjectKey,
            Instant createdAt
    ) {
        ChatMessage message = new ChatMessage(
                chatRoom,
                sender,
                ChatMessageType.ATTACHMENT,
                optionalCaption(caption),
                createdAt
        );

        message.attachment = new ChatAttachment(
                message,
                originalFilename,
                contentType,
                fileSizeBytes,
                s3Bucket,
                s3ObjectKey,
                createdAt
        );

        return message;
    }

    private ChatMessage(ChatRoom chatRoom, AppUser sender, ChatMessageType type, String body, Instant createdAt) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chat room is required");
        }

        if (sender == null) {
            throw new IllegalArgumentException("Sender is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Message type is required");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        if (chatRoom.isClosed()) {
            throw new IllegalArgumentException("Closed chat rooms cannot receive new messages");
        }

        if (!chatRoom.hasActiveParticipant(sender)) {
            throw new IllegalArgumentException("Sender must be an active chat room participant");
        }

        if (chatRoom.activeParticipantCount() < 2) {
            throw new IllegalArgumentException("At least two active participants are required to send messages");
        }

        this.chatRoom = chatRoom;
        this.sender = sender;
        this.type = type;
        this.body = body;
        this.createdAt = createdAt;
    }

    private static String requiredBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body is required");
        }

        return normalizeBody(body);
    }

    private static String optionalCaption(String caption) {
        if (caption == null || caption.isBlank()) {
            return null;
        }

        return normalizeBody(caption);
    }

    private static String normalizeBody(String body) {
        String normalized = body.trim();

        if (normalized.length() > MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Message body cannot exceed 1000 characters");
        }

        return normalized;
    }

    boolean isAttachment() {
        return type == ChatMessageType.ATTACHMENT;
    }

    UUID getId() {
        return id;
    }

    ChatRoom getChatRoom() {
        return chatRoom;
    }

    AppUser getSender() {
        return sender;
    }

    ChatMessageType getType() {
        return type;
    }

    String getBody() {
        return body;
    }

    ChatAttachment getAttachment() {
        return attachment;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}