package com.opencircle.engagement;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

record EngagementRequestResponse(
        UUID id,
        UUID invitePostId,
        UUID requesterId,
        String requesterUsername,
        ProfileImageResponse requesterProfileImage,
        EngagementRequestStatus status,
        Instant expiresAt,
        Instant respondedAt,
        Instant withdrawnAt,
        Instant createdAt,
        Instant updatedAt
) {

    static EngagementRequestResponse from(EngagementRequest request) {
        return from(request, null);
    }

    static EngagementRequestResponse from(
            EngagementRequest request,
            ProfileImageResponse requesterProfileImage
    ) {
        return new EngagementRequestResponse(
                request.getId(),
                request.getInvitePost().getId(),
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                requesterProfileImage,
                request.getStatus(),
                request.getExpiresAt(),
                request.getRespondedAt(),
                request.getWithdrawnAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
