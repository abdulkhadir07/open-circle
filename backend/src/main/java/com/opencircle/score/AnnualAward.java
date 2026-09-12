package com.opencircle.score;

import com.opencircle.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "annual_awards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_annual_awards_season_winner",
                columnNames = {"season_year", "winner_user_id"}
        )
)
class AnnualAward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_year", nullable = false, updatable = false)
    private AnnualAwardFinalization finalization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "winner_user_id", nullable = false, updatable = false)
    private AppUser winner;

    @Column(name = "final_score", nullable = false, updatable = false)
    private long finalScore;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private Instant awardedAt;

    protected AnnualAward() {
    }

    AnnualAward(
            AnnualAwardFinalization finalization,
            AppUser winner,
            long finalScore,
            Instant awardedAt
    ) {
        if (finalization == null) {
            throw new IllegalArgumentException("Award finalization is required");
        }
        if (winner == null) {
            throw new IllegalArgumentException("Award winner is required");
        }
        if (finalScore <= 0) {
            throw new IllegalArgumentException("Award score must be positive");
        }
        if (awardedAt == null) {
            throw new IllegalArgumentException("Awarded time is required");
        }

        this.finalization = finalization;
        this.winner = winner;
        this.finalScore = finalScore;
        this.awardedAt = awardedAt;
    }

    UUID getId() {
        return id;
    }

    int getSeasonYear() {
        return finalization.getSeasonYear();
    }

    AppUser getWinner() {
        return winner;
    }

    long getFinalScore() {
        return finalScore;
    }

    Instant getAwardedAt() {
        return awardedAt;
    }
}
