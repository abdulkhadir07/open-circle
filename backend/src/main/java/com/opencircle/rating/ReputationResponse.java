package com.opencircle.rating;

import com.opencircle.profileimage.ProfileImageResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ReputationResponse(
        UUID userId,
        String username,
        ProfileImageResponse profileImage,
        BigDecimal averageRating,
        long totalRatingsReceived,
        long distinctRaterCount
) {
}
