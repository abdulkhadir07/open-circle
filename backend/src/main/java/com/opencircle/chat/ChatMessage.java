package com.opencircle.chat;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatMessage() {
    }

    ChatMessage(ChatRoom chatRoom, AppUser sender, String body, Instant createdAt) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chat room is required");
        }

        if (sender == null) {
            throw new IllegalArgumentException("Sender is required");
        }

        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Message body is required");
        }

        if (!chatRoom.hasParticipant(sender)) {
            throw new IllegalArgumentException("Sender must be a chat room participant");
        }

        this.chatRoom = chatRoom;
        this.sender = sender;
        this.body = body.trim();
        this.createdAt = createdAt;
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

    String getBody() {
        return body;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}