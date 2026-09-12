package com.opencircle.score;

import java.time.Instant;

public record EarnedAnnualAward(
        int seasonYear,
        long finalScore,
        Instant awardedAt
) {
}
