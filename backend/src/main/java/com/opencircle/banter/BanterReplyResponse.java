package com.opencircle.banter;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

record BanterReplyResponse(
        UUID id,
        UUID banterId,
        UUID authorId,
        String authorUsername,
        ProfileImageResponse authorProfileImage,
        String content,
        Instant createdAt,
        boolean mine
) {

    static BanterReplyResponse from(BanterReply reply, ProfileImageResponse authorProfileImage, UUID viewerId) {
        return new BanterReplyResponse(
                reply.getId(),
                reply.getBanter().getId(),
                reply.getAuthor().getId(),
                reply.getAuthor().getUsername(),
                authorProfileImage,
                reply.getContent(),
                reply.getCreatedAt(),
                reply.getAuthor().getId().equals(viewerId)
        );
    }
}
