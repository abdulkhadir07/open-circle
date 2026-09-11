package com.opencircle.rating;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

public record DueRatingResponse(
        UUID obligationId,
        UUID engagementId,
        UUID otherUserId,
        String otherUsername,
        ProfileImageResponse otherUserProfileImage,
        RatingTrigger trigger,
        Instant requiredAt,
        Instant dueAt
) {

    static DueRatingResponse from(RatingObligation obligation, ProfileImageResponse profileImage) {
        return new DueRatingResponse(
                obligation.getId(),
                obligation.getEngagementRequest().getId(),
                obligation.getRatedUser().getId(),
                obligation.getRatedUser().getUsername(),
                profileImage,
                obligation.getTrigger(),
                obligation.getRequiredAt(),
                obligation.getDueAt()
        );
    }
}
