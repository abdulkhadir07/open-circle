package com.opencircle.ai;

import java.util.List;

// totalCapacity is only set for GROUP invites.
record InviteDraftResponse(
        String content,
        String inviteType,
        Integer totalCapacity,
        List<String> tags,
        boolean aiGenerated
) {
}
