package com.opencircle.invitepost;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A small public window onto the local and global feeds, because the invite post service itself is package-private.
@Service
public class LocalGlobalFeedQuery {

    private final InvitePostService invitePostService;

    LocalGlobalFeedQuery(InvitePostService invitePostService) {
        this.invitePostService = invitePostService;
    }

    // The newest open invites the viewer can see (local and global), up to the limit.
    // Viewers without a verified location see no feed yet, so this returns nothing for them.
    @Transactional(readOnly = true)
    public List<FeedInvite> openInvites(AppUser viewer, int limit) {
        if (!viewer.hasVerifiedLocation()) {
            return List.of();
        }

        Map<UUID, InvitePost> visible = new LinkedHashMap<>();
        for (InvitePost post : invitePostService.getLocalFeed(viewer, null)) {
            visible.put(post.getId(), post);
        }
        for (InvitePost post : invitePostService.getGlobalFeed(viewer)) {
            visible.put(post.getId(), post);
        }

        return visible.values().stream()
                .sorted(Comparator.comparing(InvitePost::getCreatedAt).reversed())
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
