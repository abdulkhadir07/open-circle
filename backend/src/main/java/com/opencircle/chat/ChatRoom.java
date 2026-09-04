package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Duration;
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

    private static final Duration AUTO_CLOSE_DELAY = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_post_id", nullable = false)
    private InvitePost invitePost;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ChatRoomParticipant> participants = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

    @Column(name = "saved_at")
    private Instant savedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saved_by_id")
    private AppUser savedBy;

    @Column(name = "auto_close_at")
    private Instant autoCloseAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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

        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
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
        refreshAutoCloseState(joinedAt);
    }

    // Saves the room permanently so participant exits no longer trigger auto-close.
    void save(AppUser user, Instant savedAt) {
        if (isClosed()) {
            throw new IllegalStateException("Closed chat rooms cannot be saved");
        }

        if (!hasActiveParticipant(user)) {
            throw new IllegalStateException("Only active participants can save the chat room");
        }

        if (isSaved()) {
            return;
        }

        this.savedAt = savedAt;
        this.savedBy = user;
        this.autoCloseAt = null;
        this.updatedAt = savedAt;
    }

    // Marks the participant as left and recalculates whether the room should auto-close.
    void leave(AppUser user, Instant leftAt) {
        ChatRoomParticipant participant = participantFor(user);
        participant.leave(leftAt);
        refreshAutoCloseState(leftAt);
    }

    // Lets the poster remove another participant from the room.
    void removeParticipant(AppUser user, AppUser removedBy, Instant removedAt) {
        if (!sameUser(invitePost.getPoster(), removedBy)) {
            throw new IllegalStateException("Only the poster can remove chat room participants");
        }

        if (sameUser(user, removedBy)) {
            throw new IllegalStateException("Poster must leave the chat room instead of removing themselves");
        }

        ChatRoomParticipant participant = participantFor(user);
        participant.remove(removedBy, removedAt);
        refreshAutoCloseState(removedAt);
    }

    // Hides the room from one participant's room list without deleting it for others.
    void hideFor(AppUser user, Instant hiddenAt) {
        participantFor(user).hide(hiddenAt);
    }

    // Archives the room as read-only and clears any pending auto-close deadline.
    void close(Instant closedAt) {
        if (isClosed()) {
            return;
        }

        status = ChatRoomStatus.CLOSED;
        this.closedAt = closedAt;
        autoCloseAt = null;
        updatedAt = closedAt;
    }

    boolean hasParticipant(AppUser user) {
        return participants.stream()
                .anyMatch(participant -> participant.belongsTo(user));
    }

    boolean hasActiveParticipant(AppUser user) {
        return participants.stream()
                .anyMatch(participant -> participant.belongsTo(user) && participant.isActive());
    }

    boolean isSaved() {
        return savedAt != null;
    }

    boolean isClosed() {
        return status == ChatRoomStatus.CLOSED;
    }

    boolean shouldAutoClose(Instant now) {
        return status == ChatRoomStatus.ACTIVE
                && !isSaved()
                && autoCloseAt != null
                && !autoCloseAt.isAfter(now);
    }

    long activeParticipantCount() {
        return participants.stream()
                .filter(ChatRoomParticipant::isActive)
                .count();
    }

    // Updates the room activity timestamp when a new message is sent.
    void recordMessageSent(Instant messageSentAt) {
        if (messageSentAt == null) {
            throw new IllegalArgumentException("Message sent time is required");
        }

        updatedAt = messageSentAt;
    }

    private void refreshAutoCloseState(Instant now) {
        if (isClosed() || isSaved()) {
            autoCloseAt = null;
            updatedAt = now;
            return;
        }

        if (shouldStartAutoCloseCountdown()) {
            if (autoCloseAt == null) {
                autoCloseAt = now.plus(AUTO_CLOSE_DELAY);
            }
        } else {
            autoCloseAt = null;
        }

        updatedAt = now;
    }

    private boolean shouldStartAutoCloseCountdown() {
        return !hasActiveParticipant(invitePost.getPoster()) || activeParticipantCount() <= 1;
    }

    private ChatRoomParticipant participantFor(AppUser user) {
        return participants.stream()
                .filter(participant -> participant.belongsTo(user))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Chat room participant is required"));
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first != null
                && second != null
                && first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
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

    ChatRoomStatus getStatus() {
        return status;
    }

    Instant getSavedAt() {
        return savedAt;
    }

    AppUser getSavedBy() {
        return savedBy;
    }

    Instant getAutoCloseAt() {
        return autoCloseAt;
    }

    Instant getClosedAt() {
        return closedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}