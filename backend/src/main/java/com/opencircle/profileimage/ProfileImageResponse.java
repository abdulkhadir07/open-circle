package com.opencircle.profileimage;

import com.opencircle.storage.StorageAccessUrl;

import java.time.Instant;
import java.util.UUID;

public record ProfileImageResponse(
        UUID id,
        String url,
        Instant urlExpiresAt,
        String contentType,
        Instant updatedAt
) {

    static ProfileImageResponse from(ProfileImage image, StorageAccessUrl accessUrl) {
        return new ProfileImageResponse(
                image.getId(),
                accessUrl.url().toString(),
                accessUrl.expiresAt(),
                image.getContentType(),
                image.getUpdatedAt()
        );
    }
}
