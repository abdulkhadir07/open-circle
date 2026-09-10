package com.opencircle.invitepost;

import com.opencircle.invitepost.image.InvitePostImageResponse;
import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record InvitePostResponse(
        UUID id,
        UUID posterId,
        String posterUsername,
        ProfileImageResponse posterProfileImage,
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
        Instant updatedAt,
        List<InvitePostImageResponse> images
) {

    static InvitePostResponse from(InvitePost post) {
        return from(post, null, List.of());
    }

    static InvitePostResponse from(InvitePost post, List<InvitePostImageResponse> images) {
        return from(post, null, images);
    }

    static InvitePostResponse from(
            InvitePost post,
            ProfileImageResponse posterProfileImage,
            List<InvitePostImageResponse> images
    ) {
        return new InvitePostResponse(
                post.getId(),
                post.getPoster().getId(),
                post.getPoster().getUsername(),
                posterProfileImage,
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
                post.getUpdatedAt(),
                List.copyOf(images)
        );
    }
}
