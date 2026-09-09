package com.opencircle.storage;

import java.net.URI;
import java.time.Instant;

public record StorageAccessUrl(
        URI url,
        Instant expiresAt
) {
}