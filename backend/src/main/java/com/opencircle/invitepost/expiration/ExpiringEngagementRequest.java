package com.opencircle.invitepost.expiration;

import java.util.UUID;

record ExpiringEngagementRequest(
        UUID requestId,
        UUID requesterUserId
) {

    ExpiringEngagementRequest {
        if (requestId == null || requesterUserId == null) {
            throw new IllegalArgumentException("Expiring engagement request fields are required");
        }
    }
}
