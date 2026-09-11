package com.opencircle.rating;

import java.time.Instant;
import java.util.UUID;

record RatingLifecycleSnapshot(
        UUID engagementId,
        UUID invitePostId,
        UUID posterUserId,
        UUID requesterUserId,
        Instant acceptedAt,
        Instant lastRoomActivityAt,
        long pairMessageCount,
        long posterMessageCount,
        long requesterMessageCount
) {

    boolean isQualified() {
        return pairMessageCount >= 3 && posterMessageCount > 0 && requesterMessageCount > 0;
    }
}
