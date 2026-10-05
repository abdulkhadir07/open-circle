package com.opencircle.invitepost;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

// A small public window onto the campus feed, because the invite post service itself is package-private.
@Service
public class CampusFeedQuery {

    private final InvitePostRepository posts;
    private final Clock clock;

    CampusFeedQuery(InvitePostRepository posts, Clock clock) {
        this.posts = posts;
        this.clock = clock;
    }

    // The newest open invites on the viewer's campus, up to the limit.
    @Transactional(readOnly = true)
    public List<FeedInvite> openInvites(AppUser viewer, int limit) {
        return posts.findCampusFeed(InvitePostStatus.ACTIVE, Instant.now(clock), viewer.getCampus()).stream()
                .limit(limit)
                .map(post -> new FeedInvite(
                        post.getId(),
                        post.getPoster().getId(),
                        post.getContent(),
                        post.getTags(),
                        post.getInviteType().name(),
                        post.getInvitesLeft()
                ))
                .toList();
    }
}
