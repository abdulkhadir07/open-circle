package com.opencircle.session;

import java.time.Instant;
import java.util.UUID;

public record SessionDetails(
        UUID id,
        String userAgent,
        boolean current,
        Instant createdAt,
        Instant lastUsedAt,
        Instant inactiveAt,
        Instant expiresAt
) {
}
