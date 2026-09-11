package com.opencircle.rating;

import java.time.Instant;
import java.util.UUID;

public record RatingSubmissionResponse(
        UUID id,
        UUID engagementId,
        UUID ratedUserId,
        int score,
        Instant submittedAt,
        boolean revealed,
        Instant revealedAt
) {

    static RatingSubmissionResponse from(Rating rating) {
        return new RatingSubmissionResponse(
                rating.getId(),
                rating.getObligation().getEngagementRequest().getId(),
                rating.getObligation().getRatedUser().getId(),
                rating.getScore(),
                rating.getSubmittedAt(),
                rating.getRevealedAt() != null,
                rating.getRevealedAt()
        );
    }
}
