package com.opencircle.chat;

import com.opencircle.storage.AttachmentDownloadUrl;

import java.time.Instant;

public record AttachmentDownloadUrlResponse(
        String url,
        Instant expiresAt
) {

    static AttachmentDownloadUrlResponse from(AttachmentDownloadUrl downloadUrl) {
        return new AttachmentDownloadUrlResponse(
                downloadUrl.url().toString(),
                downloadUrl.expiresAt()
        );
    }
}