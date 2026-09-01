package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.location.LocationNotVerifiedException;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EngagementRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    private final EngagementRequestRepository requests = mock(EngagementRequestRepository.class);
    private final InvitePostRepository posts = mock(InvitePostRepository.class);
    private final EngagementRequestService service = new EngagementRequestService(
            requests,
            posts,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createRequestCreatesPendingRequestForOpenPost() {
        AppUser poster = verifiedUser("poster@example.com");
        AppUser requester = verifiedUser("requester@example.com");
        InvitePost post = invitePost(poster);

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(requests.existsByInvitePostAndRequester(post, requester)).thenReturn(false);
        when(requests.save(any(EngagementRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EngagementRequest request = service.createRequest(requester, post.getId());

        assertThat(request.getInvitePost()).isEqualTo(post);
        assertThat(request.getRequester()).isEqualTo(requester);
        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.PENDING);
        assertThat(request.getCreatedAt()).isEqualTo(NOW);
        assertThat(request.getExpiresAt()).isEqualTo(post.getExpiresAt());

        verify(requests).save(request);
    }

    @Test
    void createRequestRejectsUnverifiedLocation() {
        AppUser requester = user("requester.unverified@example.com");

        assertThatThrownBy(() -> service.createRequest(requester, UUID.randomUUID()))
                .isInstanceOf(LocationNotVerifiedException.class);

        verify(posts, never()).findById(any());
        verify(requests, never()).save(any());
    }

    @Test
    void createRequestRejectsOwnPost() {
        AppUser poster = verifiedUser("poster.own@example.com");
        InvitePost post = invitePost(poster);

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.createRequest(poster, post.getId()))
                .isInstanceOf(CannotEngageOwnPostException.class)
                .hasMessage("You cannot engage with your own invite post");

        verify(requests, never()).save(any());
    }

    @Test
    void createRequestRejectsDuplicateRequest() {
        AppUser poster = verifiedUser("poster.duplicate@example.com");
        AppUser requester = verifiedUser("requester.duplicate@example.com");
        InvitePost post = invitePost(poster);

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(requests.existsByInvitePostAndRequester(post, requester)).thenReturn(true);

        assertThatThrownBy(() -> service.createRequest(requester, post.getId()))
                .isInstanceOf(DuplicateEngagementRequestException.class)
                .hasMessage("You have already requested to engage with this post");

        verify(requests, never()).save(any());
    }

    @Test
    void createRequestRejectsClosedPost() {
        AppUser poster = verifiedUser("poster.closed@example.com");
        AppUser requester = verifiedUser("requester.closed@example.com");
        InvitePost post = invitePost(poster);
        post.close();

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.createRequest(requester, post.getId()))
                .isInstanceOf(EngagementRequestNotActionableException.class)
                .hasMessage("Invite post is not open for engagement requests");

        verify(requests, never()).save(any());
    }

    @Test
    void getRequestsForPostReturnsRequestsForOwnedPost() {
        AppUser poster = verifiedUser("poster.requests@example.com");
        AppUser requester = verifiedUser("requester.requests@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW);

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));
        when(requests.findByInvitePostOrderByCreatedAtDesc(post)).thenReturn(List.of(request));

        List<EngagementRequest> result = service.getRequestsForPost(poster, post.getId());

        assertThat(result).containsExactly(request);
    }

    @Test
    void getRequestsForPostRejectsNonOwner() {
        AppUser poster = verifiedUser("poster.nonowner@example.com");
        AppUser otherUser = verifiedUser("other.nonowner@example.com");
        InvitePost post = invitePost(poster);

        when(posts.findById(post.getId())).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.getRequestsForPost(otherUser, post.getId()))
                .isInstanceOf(EngagementForbiddenException.class)
                .hasMessage("Only the post owner can manage engagement requests");
    }

    @Test
    void acceptRequestMarksAcceptedAndConsumesCapacity() {
        AppUser poster = verifiedUser("poster.accept@example.com");
        AppUser requester = verifiedUser("requester.accept@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW.minusSeconds(60));

        when(requests.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        EngagementRequest result = service.acceptRequest(poster, request.getId());

        assertThat(result.getStatus()).isEqualTo(EngagementRequestStatus.ACCEPTED);
        assertThat(result.getRespondedAt()).isEqualTo(NOW);
        assertThat(post.getAcceptedCount()).isEqualTo(1);
        assertThat(post.getInvitesLeft()).isEqualTo(2);
    }

    @Test
    void acceptRequestRejectsWhenPostIsAlreadyFull() {
        AppUser poster = verifiedUser("poster.full@example.com");
        AppUser firstRequester = verifiedUser("first.full@example.com");
        AppUser secondRequester = verifiedUser("second.full@example.com");

        InvitePost post = new InvitePost(
                poster,
                "One person only",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(60)
        );

        EngagementRequest acceptedRequest = new EngagementRequest(post, firstRequester, NOW.minusSeconds(50));
        acceptedRequest.accept(NOW.minusSeconds(40));
        post.recordAcceptedEngagement();

        EngagementRequest pendingRequest = new EngagementRequest(post, secondRequester, NOW.minusSeconds(30));

        when(requests.findDetailedById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> service.acceptRequest(poster, pendingRequest.getId()))
                .isInstanceOf(EngagementRequestNotActionableException.class)
                .hasMessage("Invite post is not open for engagement requests");

        assertThat(post.getAcceptedCount()).isEqualTo(1);
    }

    @Test
    void declineRequestMarksDeclinedWithoutChangingCapacity() {
        AppUser poster = verifiedUser("poster.decline@example.com");
        AppUser requester = verifiedUser("requester.decline@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW.minusSeconds(60));

        when(requests.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        EngagementRequest result = service.declineRequest(poster, request.getId());

        assertThat(result.getStatus()).isEqualTo(EngagementRequestStatus.DECLINED);
        assertThat(result.getRespondedAt()).isEqualTo(NOW);
        assertThat(post.getAcceptedCount()).isZero();
    }

    @Test
    void holdRequestMarksHeldWithoutChangingCapacity() {
        AppUser poster = verifiedUser("poster.hold@example.com");
        AppUser requester = verifiedUser("requester.hold@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW.minusSeconds(60));

        when(requests.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        EngagementRequest result = service.holdRequest(poster, request.getId());

        assertThat(result.getStatus()).isEqualTo(EngagementRequestStatus.HELD);
        assertThat(result.getRespondedAt()).isEqualTo(NOW);
        assertThat(post.getAcceptedCount()).isZero();
    }

    @Test
    void withdrawRequestMarksPendingRequestWithdrawn() {
        AppUser poster = verifiedUser("poster.withdraw@example.com");
        AppUser requester = verifiedUser("requester.withdraw@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW.minusSeconds(60));

        when(requests.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        EngagementRequest result = service.withdrawRequest(requester, request.getId());

        assertThat(result.getStatus()).isEqualTo(EngagementRequestStatus.WITHDRAWN);
        assertThat(result.getWithdrawnAt()).isEqualTo(NOW);
    }

    @Test
    void withdrawRequestRejectsNonRequester() {
        AppUser poster = verifiedUser("poster.withdraw.forbidden@example.com");
        AppUser requester = verifiedUser("requester.withdraw.forbidden@example.com");
        AppUser otherUser = verifiedUser("other.withdraw.forbidden@example.com");
        InvitePost post = invitePost(poster);
        EngagementRequest request = new EngagementRequest(post, requester, NOW.minusSeconds(60));

        when(requests.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.withdrawRequest(otherUser, request.getId()))
                .isInstanceOf(EngagementForbiddenException.class)
                .hasMessage("Only the requester can withdraw this engagement request");
    }

    private InvitePost invitePost(AppUser poster) {
        return new InvitePost(
                poster,
                "Anyone want to hang out?",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(60)
        );
    }

    private AppUser verifiedUser(String email) {
        AppUser user = user(email);
        user.verifyLocation("San Francisco", "California", "USA", NOW.minusSeconds(120));
        return user;
    }

    private AppUser user(String email) {
        return new AppUser(
                email.substring(0, email.indexOf("@")).replace(".", "_") + "_1234",
                "Test",
                "User",
                email,
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}