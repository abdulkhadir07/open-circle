package com.opencircle.storage;

import java.net.URI;
import java.time.Instant;

public record AttachmentDownloadUrl(
        URI url,
        Instant expiresAt
) {
}