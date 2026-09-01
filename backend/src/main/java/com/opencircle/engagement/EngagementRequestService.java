package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.location.LocationNotVerifiedException;
import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class EngagementRequestService {

    private final EngagementRequestRepository requests;
    private final InvitePostRepository posts;
    private final Clock clock;

    EngagementRequestService(
            EngagementRequestRepository requests,
            InvitePostRepository posts,
            Clock clock
    ) {
        this.requests = requests;
        this.posts = posts;
        this.clock = clock;
    }

    // Creates a pending engagement request when the requester is eligible for the invite post.
    @Transactional
    EngagementRequest createRequest(AppUser requester, UUID invitePostId) {
        requireVerifiedLocation(requester);

        InvitePost post = posts.findById(invitePostId)
                .orElseThrow(() -> new EngagementRequestNotActionableException("Invite post is not available"));

        Instant now = Instant.now(clock);

        requireNotOwnPost(post, requester);
        requirePostOpen(post, now);
        requireNoDuplicateRequest(post, requester);

        EngagementRequest request = new EngagementRequest(post, requester, now);
        return requests.save(request);
    }

    // Returns all engagement requests for a post owned by the current user.
    @Transactional(readOnly = true)
    List<EngagementRequest> getRequestsForPost(AppUser poster, UUID invitePostId) {
        InvitePost post = posts.findById(invitePostId)
                .orElseThrow(() -> new EngagementRequestNotActionableException("Invite post is not available"));

        requirePostOwner(post, poster);

        return requests.findByInvitePostOrderByCreatedAtDesc(post);
    }

    // Accepts a request and consumes one invite capacity slot.
    @Transactional
    EngagementRequest acceptRequest(AppUser poster, UUID requestId) {
        EngagementRequest request = detailedRequest(requestId);
        InvitePost post = request.getInvitePost();
        Instant now = Instant.now(clock);

        requirePostOwner(post, poster);
        requirePostOpen(post, now);

        try {
            request.accept(now);
            post.recordAcceptedEngagement();
        } catch (IllegalStateException exception) {
            throw new EngagementRequestNotActionableException(exception.getMessage());
        }

        return request;
    }

    // Declines a pending or held request without changing invite capacity.
    @Transactional
    EngagementRequest declineRequest(AppUser poster, UUID requestId) {
        EngagementRequest request = detailedRequest(requestId);
        InvitePost post = request.getInvitePost();
        Instant now = Instant.now(clock);

        requirePostOwner(post, poster);

        try {
            request.decline(now);
        } catch (IllegalStateException exception) {
            throw new EngagementRequestNotActionableException(exception.getMessage());
        }

        return request;
    }

    // Holds a request until the invite post expires or the poster makes a final decision.
    @Transactional
    EngagementRequest holdRequest(AppUser poster, UUID requestId) {
        EngagementRequest request = detailedRequest(requestId);
        InvitePost post = request.getInvitePost();
        Instant now = Instant.now(clock);

        requirePostOwner(post, poster);

        try {
            request.hold(now);
        } catch (IllegalStateException exception) {
            throw new EngagementRequestNotActionableException(exception.getMessage());
        }

        return request;
    }

    // Allows the requester to cancel their own pending or held request.
    @Transactional
    EngagementRequest withdrawRequest(AppUser requester, UUID requestId) {
        EngagementRequest request = detailedRequest(requestId);

        requireRequester(request, requester);

        try {
            request.withdraw(Instant.now(clock));
        } catch (IllegalStateException exception) {
            throw new EngagementRequestNotActionableException(exception.getMessage());
        }

        return request;
    }

    private EngagementRequest detailedRequest(UUID requestId) {
        return requests.findDetailedById(requestId)
                .orElseThrow(EngagementRequestNotFoundException::new);
    }

    private void requireVerifiedLocation(AppUser user) {
        if (!user.hasVerifiedLocation()) {
            throw new LocationNotVerifiedException();
        }
    }

    private void requireNoDuplicateRequest(InvitePost post, AppUser requester) {
        if (requests.existsByInvitePostAndRequester(post, requester)) {
            throw new DuplicateEngagementRequestException();
        }
    }

    private void requireNotOwnPost(InvitePost post, AppUser requester) {
        if (sameUser(post.getPoster(), requester)) {
            throw new CannotEngageOwnPostException();
        }
    }

    private void requirePostOwner(InvitePost post, AppUser user) {
        if (!sameUser(post.getPoster(), user)) {
            throw new EngagementForbiddenException("Only the post owner can manage engagement requests");
        }
    }

    private void requireRequester(EngagementRequest request, AppUser user) {
        if (!sameUser(request.getRequester(), user)) {
            throw new EngagementForbiddenException("Only the requester can withdraw this engagement request");
        }
    }

    private void requirePostOpen(InvitePost post, Instant now) {
        if (!post.isOpen(now)) {
            throw new EngagementRequestNotActionableException("Invite post is not open for engagement requests");
        }
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
    }
}