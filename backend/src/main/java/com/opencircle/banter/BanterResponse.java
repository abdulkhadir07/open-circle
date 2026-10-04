package com.opencircle.banter;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

record BanterResponse(
        UUID id,
        UUID authorId,
        String authorUsername,
        ProfileImageResponse authorProfileImage,
        String content,
        Instant createdAt,
        long likeCount,
        long replyCount,
        boolean likedByMe,
        boolean mine
) {

    static BanterResponse from(BanterView view, ProfileImageResponse authorProfileImage, UUID viewerId) {
        Banter banter = view.banter();

        return new BanterResponse(
                banter.getId(),
                banter.getAuthor().getId(),
                banter.getAuthor().getUsername(),
                authorProfileImage,
                banter.getContent(),
                banter.getCreatedAt(),
                view.likeCount(),
                view.replyCount(),
                view.likedByMe(),
                banter.getAuthor().getId().equals(viewerId)
        );
    }
}
