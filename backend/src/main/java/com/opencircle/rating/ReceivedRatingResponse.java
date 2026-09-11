package com.opencircle.rating;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

public record ReceivedRatingResponse(
        UUID id,
        UUID engagementId,
        UUID raterUserId,
        String raterUsername,
        ProfileImageResponse raterProfileImage,
        int score,
        Instant revealedAt
) {

    static ReceivedRatingResponse from(Rating rating, ProfileImageResponse profileImage) {
        return new ReceivedRatingResponse(
                rating.getId(),
                rating.getObligation().getEngagementRequest().getId(),
                rating.getObligation().getRater().getId(),
                rating.getObligation().getRater().getUsername(),
                profileImage,
                rating.getScore(),
                rating.getRevealedAt()
        );
    }
}
