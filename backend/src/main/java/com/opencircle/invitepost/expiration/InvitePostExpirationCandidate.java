package com.opencircle.invitepost.expiration;

import java.time.Instant;
import java.util.UUID;

record InvitePostExpirationCandidate(
        UUID postId,
        UUID posterUserId,
        Instant expiresAt
) {

    InvitePostExpirationCandidate {
        if (postId == null || posterUserId == null || expiresAt == null) {
            throw new IllegalArgumentException("Expiration candidate fields are required");
        }
    }
}
