package com.opencircle.rating;

import java.time.Instant;
import java.util.UUID;

public record RatingContribution(
        UUID ratingId,
        UUID engagementId,
        UUID raterUserId,
        UUID ratedUserId,
        int score,
        Instant revealedAt,
        int seasonYear
) {
}
