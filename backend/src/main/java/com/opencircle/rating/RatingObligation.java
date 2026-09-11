package com.opencircle.rating;

import com.opencircle.engagement.EngagementRequest;
import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "rating_obligations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rating_obligations_engagement_rater",
                columnNames = {"engagement_request_id", "rater_user_id"}
        )
)
class RatingObligation {

    private static final Duration SUBMISSION_WINDOW = Duration.ofHours(24);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_request_id", nullable = false)
    private EngagementRequest engagementRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rater_user_id", nullable = false)
    private AppUser rater;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rated_user_id", nullable = false)
    private AppUser ratedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RatingObligationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_trigger", length = 30)
    private RatingTrigger trigger;

    @Column(name = "required_at")
    private Instant requiredAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "penalty_points")
    private Integer penaltyPoints;

    @Column(name = "penalized_at")
    private Instant penalizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RatingObligation() {
    }

    RatingObligation(EngagementRequest engagementRequest, AppUser rater, AppUser ratedUser, Instant createdAt) {
        if (engagementRequest == null) {
            throw new IllegalArgumentException("Engagement request is required");
        }
        if (rater == null || ratedUser == null) {
            throw new IllegalArgumentException("Both rating users are required");
        }
        if (sameUser(rater, ratedUser)) {
            throw new IllegalArgumentException("Users cannot rate themselves");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created time is required");
        }

        this.engagementRequest = engagementRequest;
        this.rater = rater;
        this.ratedUser = ratedUser;
        this.status = RatingObligationStatus.MONITORING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void require(RatingTrigger trigger, Instant requiredAt) {
        if (status != RatingObligationStatus.MONITORING) {
            return;
        }
        if (trigger == null || requiredAt == null) {
            throw new IllegalArgumentException("Rating requirement trigger and time are required");
        }

        this.status = RatingObligationStatus.REQUIRED;
        this.trigger = trigger;
        this.requiredAt = requiredAt;
        this.dueAt = requiredAt.plus(SUBMISSION_WINDOW);
        this.updatedAt = requiredAt;
    }

    void markNotRequired(Instant decidedAt) {
        if (status != RatingObligationStatus.MONITORING) {
            return;
        }
        if (decidedAt == null) {
            throw new IllegalArgumentException("Decision time is required");
        }

        status = RatingObligationStatus.NOT_REQUIRED;
        updatedAt = decidedAt;
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
    }

    UUID getId() {
        return id;
    }

    EngagementRequest getEngagementRequest() {
        return engagementRequest;
    }

    AppUser getRater() {
        return rater;
    }

    AppUser getRatedUser() {
        return ratedUser;
    }

    RatingObligationStatus getStatus() {
        return status;
    }

    RatingTrigger getTrigger() {
        return trigger;
    }

    Instant getRequiredAt() {
        return requiredAt;
    }

    Instant getDueAt() {
        return dueAt;
    }

    Instant getSubmittedAt() {
        return submittedAt;
    }

    Integer getPenaltyPoints() {
        return penaltyPoints;
    }

    Instant getPenalizedAt() {
        return penalizedAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }
}
