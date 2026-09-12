package com.opencircle.user;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String username,
        String displayName,
        ProfileImageResponse profileImage,
        String bio,
        List<String> interests,
        Instant memberSince,
        ProfileReputationResponse reputation,
        List<UserProfileAwardResponse> awards
) {
    public UserProfileResponse {
        interests = List.copyOf(interests);
        awards = List.copyOf(awards);
    }
}
