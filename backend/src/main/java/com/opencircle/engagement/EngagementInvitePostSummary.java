package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;

import java.util.UUID;

record EngagementInvitePostSummary(
        UUID id,
        String content,
        String posterUsername,
        String city,
        String stateRegion,
        String country
) {

    static EngagementInvitePostSummary from(InvitePost post) {
        return new EngagementInvitePostSummary(
                post.getId(),
                post.getContent(),
                post.getPoster().getUsername(),
                post.getCity(),
                post.getStateRegion(),
                post.getCountry()
        );
    }
}