package com.opencircle.rating;

import java.time.Instant;
import java.util.UUID;

record RevealedRating(
        UUID ratingId,
        UUID engagementId,
        UUID invitePostId,
        UUID raterUserId,
        UUID ratedUserId,
        Instant revealedAt
) {
}
