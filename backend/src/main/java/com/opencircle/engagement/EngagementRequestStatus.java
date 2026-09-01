package com.opencircle.engagement;

// Tracks the lifecycle of a user's request to engage with an invite post.
enum EngagementRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    HELD,
    WITHDRAWN
}