package com.opencircle.session;

import java.time.Instant;
import java.util.UUID;

interface ActiveSessionProjection {

    UUID getId();

    String getUserAgent();

    Instant getCreatedAt();

    Instant getLastUsedAt();

    Instant getInactiveAt();

    Instant getExpiresAt();
}
