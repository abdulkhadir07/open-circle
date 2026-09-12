package com.opencircle.score;

import java.util.UUID;

public record ScoreSummaryResponse(
        UUID userId,
        int seasonYear,
        long annualScore,
        long lifetimeScore
) {

    static ScoreSummaryResponse from(UUID userId, CircleScoreSummary summary) {
        return new ScoreSummaryResponse(
                userId,
                summary.seasonYear(),
                summary.annualScore(),
                summary.lifetimeScore()
        );
    }
}
