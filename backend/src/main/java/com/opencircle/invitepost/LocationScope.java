package com.opencircle.invitepost;

// Defines which feed audience can see an invite post.
public enum LocationScope {
    // Everyone on the poster's campus; the only scope new posts use.
    CAMPUS,
    CITY,
    STATE_REGION,
    COUNTRY,
    GLOBAL
}
