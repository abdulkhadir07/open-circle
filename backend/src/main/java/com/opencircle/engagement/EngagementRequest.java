package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "engagement_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_engagement_requests_post_requester",
                        columnNames = {"invite_post_id", "requester_id"}
                )
        }
)
public class EngagementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invite_post_id", nullable = false)
    private InvitePost invitePost;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EngagementRequestStatus status = EngagementRequestStatus.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EngagementRequest() {
    }

    public EngagementRequest(InvitePost invitePost, AppUser requester, Instant createdAt) {
        requireInvitePost(invitePost);
        requireRequester(requester);
        requireCreatedAt(createdAt);

        // Prevents users from creating engagement requests on invite posts they own.
        if (sameUser(invitePost.getPoster(), requester)) {
            throw new IllegalArgumentException("Requester cannot engage with their own invite post");
        }

        this.invitePost = invitePost;
        this.requester = requester;
        this.expiresAt = invitePost.getExpiresAt();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            Instant now = Instant.now();
            createdAt = now;
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isActionable(Instant now) {
        return (status == EngagementRequestStatus.PENDING || status == EngagementRequestStatus.HELD)
                && !isExpired(now);
    }

    public void accept(Instant respondedAt) {
        requireRespondedAt(respondedAt);
        requireActionable(respondedAt);

        status = EngagementRequestStatus.ACCEPTED;
        this.respondedAt = respondedAt;
    }

    public void decline(Instant respondedAt) {
        requireRespondedAt(respondedAt);
        requireActionable(respondedAt);

        status = EngagementRequestStatus.DECLINED;
        this.respondedAt = respondedAt;
    }

    public void hold(Instant respondedAt) {
        requireRespondedAt(respondedAt);
        requireActionable(respondedAt);

        status = EngagementRequestStatus.HELD;
        this.respondedAt = respondedAt;
    }

    public void withdraw(Instant withdrawnAt) {
        requireWithdrawnAt(withdrawnAt);

        if (status != EngagementRequestStatus.PENDING && status != EngagementRequestStatus.HELD) {
            throw new IllegalStateException("Only pending or held engagement requests can be withdrawn");
        }

        if (isExpired(withdrawnAt)) {
            throw new IllegalStateException("Expired engagement requests cannot be withdrawn");
        }

        status = EngagementRequestStatus.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }

    private void requireActionable(Instant now) {
        if (!isActionable(now)) {
            throw new IllegalStateException("Engagement request is not actionable");
        }
    }

    private void requireInvitePost(InvitePost invitePost) {
        if (invitePost == null) {
            throw new IllegalArgumentException("Invite post is required");
        }
    }

    private void requireRequester(AppUser requester) {
        if (requester == null) {
            throw new IllegalArgumentException("Requester is required");
        }
    }

    private void requireCreatedAt(Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at is required");
        }
    }

    private void requireRespondedAt(Instant respondedAt) {
        if (respondedAt == null) {
            throw new IllegalArgumentException("Responded at is required");
        }
    }

    private void requireWithdrawnAt(Instant withdrawnAt) {
        if (withdrawnAt == null) {
            throw new IllegalArgumentException("Withdrawn at is required");
        }
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
    }

    public UUID getId() {
        return id;
    }

    public InvitePost getInvitePost() {
        return invitePost;
    }

    public AppUser getRequester() {
        return requester;
    }

    public EngagementRequestStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}