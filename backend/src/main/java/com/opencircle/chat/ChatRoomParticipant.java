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

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "removed_at")
    private Instant removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by_id")
    private AppUser removedBy;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    protected ChatRoomParticipant() {
    }

    ChatRoomParticipant(ChatRoom chatRoom, AppUser user, Instant joinedAt) {
        if (chatRoom == null) {
            throw new IllegalArgumentException("Chat room is required");
        }

        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        if (joinedAt == null) {
            throw new IllegalArgumentException("Joined time is required");
        }

        this.chatRoom = chatRoom;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    // Marks the participant as voluntarily left.
    void leave(Instant leftAt) {
        if (!isActive()) {
            throw new IllegalStateException("Only active participants can leave the chat room");
        }

        this.leftAt = leftAt;
    }

    // Marks the participant as removed by the poster.
    void remove(AppUser removedBy, Instant removedAt) {
        if (!isActive()) {
            throw new IllegalStateException("Only active participants can be removed from the chat room");
        }

        if (removedBy == null) {
            throw new IllegalArgumentException("Removed by user is required");
        }

        this.removedBy = removedBy;
        this.removedAt = removedAt;
    }

    // Hides the chat from this user's own interface only.
    void hide(Instant hiddenAt) {
        if (this.hiddenAt != null) {
            return;
        }

        this.hiddenAt = hiddenAt;
    }

    boolean belongsTo(AppUser user) {
        if (this.user == user) {
            return true;
        }

        return user != null
                && this.user.getId() != null
                && user.getId() != null
                && this.user.getId().equals(user.getId());
    }

    boolean isActive() {
        return leftAt == null && removedAt == null;
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

    Instant getLeftAt() {
        return leftAt;
    }

    Instant getRemovedAt() {
        return removedAt;
    }

    AppUser getRemovedBy() {
        return removedBy;
    }

    Instant getHiddenAt() {
        return hiddenAt;
    }
}