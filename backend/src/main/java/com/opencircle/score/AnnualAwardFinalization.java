package com.opencircle.score;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "annual_award_finalizations")
class AnnualAwardFinalization {

    @Id
    @Column(name = "season_year", updatable = false)
    private Integer seasonYear;

    @Column(name = "finalized_at", nullable = false, updatable = false)
    private Instant finalizedAt;

    protected AnnualAwardFinalization() {
    }

    int getSeasonYear() {
        return seasonYear;
    }

    Instant getFinalizedAt() {
        return finalizedAt;
    }
}
