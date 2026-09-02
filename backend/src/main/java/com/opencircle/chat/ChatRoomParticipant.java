package com.opencircle.chat;

import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_room_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chat_room_participants_room_user",
                        columnNames = {"chat_room_id", "user_id"}
                )
        }
)
class ChatRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ChatRoomParticipant() {
    }

    ChatRoomParticipant(ChatRoom chatRoom, AppUser user, Instant joinedAt) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chat room is required");
        }

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        this.chatRoom = chatRoom;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    boolean belongsTo(AppUser user) {
        if (this.user == user) {
            return true;
        }

        return this.user.getId() != null
                && user.getId() != null
                && this.user.getId().equals(user.getId());
    }

    UUID getId() {
        return id;
    }

    ChatRoom getChatRoom() {
        return chatRoom;
    }

    AppUser getUser() {
        return user;
    }

    Instant getJoinedAt() {
        return joinedAt;
    }
}