package com.opencircle.score;

import java.util.UUID;

/**
 * {@code rank} is the user's own current-season rank at any position (ties share a
 * rank), or null when they have no positive score this season.
 */
public record ScoreSummaryResponse(
        UUID userId,
        int seasonYear,
        long annualScore,
        long lifetimeScore,
        Long rank
) {
    static ScoreSummaryResponse from(UUID userId, CircleScoreSummary summary, Long rank) {
        return new ScoreSummaryResponse(
                userId,
                summary.seasonYear(),
                summary.annualScore(),
                summary.lifetimeScore(),
                rank
        );
    }
}
