package com.opencircle.engagement;

import java.time.Instant;
import java.util.UUID;

record EngagementRequestResponse(
        UUID id,
        UUID invitePostId,
        UUID requesterId,
        String requesterUsername,
        EngagementRequestStatus status,
        Instant expiresAt,
        Instant respondedAt,
        Instant withdrawnAt,
        Instant createdAt,
        Instant updatedAt
) {

    static EngagementRequestResponse from(EngagementRequest request) {
        return new EngagementRequestResponse(
                request.getId(),
                request.getInvitePost().getId(),
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                request.getStatus(),
                request.getExpiresAt(),
                request.getRespondedAt(),
                request.getWithdrawnAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}