package com.opencircle.invitepost;

import java.time.Instant;
import java.util.UUID;

record InvitePostResponse(
        UUID id,
        UUID posterId,
        String posterUsername,
        String content,
        InviteType inviteType,
        int totalCapacity,
        int acceptedCount,
        int invitesLeft,
        LocationScope locationScope,
        String city,
        String stateRegion,
        String country,
        InvitePostStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    static InvitePostResponse from(InvitePost post) {
        return new InvitePostResponse(
                post.getId(),
                post.getPoster().getId(),
                post.getPoster().getUsername(),
                post.getContent(),
                post.getInviteType(),
                post.getTotalCapacity(),
                post.getAcceptedCount(),
                post.getInvitesLeft(),
                post.getLocationScope(),
                post.getCity(),
                post.getStateRegion(),
                post.getCountry(),
                post.getStatus(),
                post.getExpiresAt(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}