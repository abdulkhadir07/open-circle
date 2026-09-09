package com.opencircle.chat;

import com.opencircle.storage.StorageAccessUrl;

import java.time.Instant;

public record AttachmentDownloadUrlResponse(
        String url,
        Instant expiresAt
) {

    static AttachmentDownloadUrlResponse from(StorageAccessUrl downloadUrl) {
        return new AttachmentDownloadUrlResponse(
                downloadUrl.url().toString(),
                downloadUrl.expiresAt()
        );
    }
}
