package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_rooms_invite_post", columnNames = "invite_post_id")
        }
)
class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_post_id", nullable = false)
    private InvitePost invitePost;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomParticipant> participants = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ChatRoom() {
    }

    ChatRoom(InvitePost invitePost, Instant createdAt) {
        if (invitePost == null) {
            throw new IllegalArgumentException("Invite post is required");
        }

        this.invitePost = invitePost;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // Adds a user to the room once their engagement request has been accepted.
    void addParticipant(AppUser user, Instant joinedAt) {
        if (hasParticipant(user)) {
            return;
        }

        participants.add(new ChatRoomParticipant(this, user, joinedAt));
        updatedAt = joinedAt;
    }

    boolean hasParticipant(AppUser user) {
        return participants.stream()
                .anyMatch(participant -> participant.belongsTo(user));
    }

    UUID getId() {
        return id;
    }

    InvitePost getInvitePost() {
        return invitePost;
    }

    Set<ChatRoomParticipant> getParticipants() {
        return participants;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}