package com.opencircle.rating;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_ratings_obligation", columnNames = "obligation_id")
)
class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligation_id", nullable = false)
    private RatingObligation obligation;

    @Column(nullable = false)
    private int score;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "revealed_at")
    private Instant revealedAt;

    protected Rating() {
    }

    Rating(RatingObligation obligation, int score, Instant submittedAt) {
        if (obligation == null) {
            throw new IllegalArgumentException("Rating obligation is required");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("Submitted time is required");
        }

        this.obligation = obligation;
        this.score = score;
        this.submittedAt = submittedAt;
    }

    UUID getId() {
        return id;
    }

    RatingObligation getObligation() {
        return obligation;
    }

    int getScore() {
        return score;
    }

    Instant getSubmittedAt() {
        return submittedAt;
    }

    Instant getRevealedAt() {
        return revealedAt;
    }
}
