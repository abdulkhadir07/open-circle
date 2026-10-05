package com.opencircle.invitepost;

import java.util.List;
import java.util.UUID;

// A read-only snapshot of an open invite, for features outside this package (such as AI insights).
public record FeedInvite(
        UUID id,
        UUID posterId,
        String content,
        List<String> tags,
        String inviteType,
        int invitesLeft
) {
}
