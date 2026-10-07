package com.opencircle.invitepost;

// Defines which feed audience can see an invite post.
public enum LocationScope {
    // Legacy: posts from the short-lived campus-only model. Kept so old rows still load; never offered.
    CAMPUS,
    CITY,
    STATE_REGION,
    COUNTRY,
    GLOBAL
}
