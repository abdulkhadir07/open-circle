package com.opencircle.invitepost.image;

import com.opencircle.storage.StorageAccessUrl;

import java.time.Instant;
import java.util.UUID;

public record InvitePostImageResponse(
        UUID id,
        String url,
        Instant urlExpiresAt,
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        int displayOrder,
        Instant createdAt
) {

    static InvitePostImageResponse from(InvitePostImage image, StorageAccessUrl accessUrl) {
        return new InvitePostImageResponse(
                image.getId(),
                accessUrl.url().toString(),
                accessUrl.expiresAt(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getFileSizeBytes(),
                image.getDisplayOrder(),
                image.getCreatedAt()
        );
    }
}
