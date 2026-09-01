package com.opencircle.engagement;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class EngagementRequestController {

    private final CurrentUserProvider currentUserProvider;
    private final EngagementRequestService engagementRequestService;

    EngagementRequestController(
            CurrentUserProvider currentUserProvider,
            EngagementRequestService engagementRequestService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.engagementRequestService = engagementRequestService;
    }

    @PostMapping("/api/invite-posts/{postId}/engagements")
    @ResponseStatus(HttpStatus.CREATED)
    public EngagementRequestResponse createRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.createRequest(currentUser, postId);

        return EngagementRequestResponse.from(request);
    }

    @GetMapping("/api/invite-posts/{postId}/engagements")
    public List<EngagementRequestResponse> getRequestsForPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return engagementRequestService.getRequestsForPost(currentUser, postId)
                .stream()
                .map(EngagementRequestResponse::from)
                .toList();
    }

    @PatchMapping("/api/engagements/{requestId}/accept")
    public EngagementRequestResponse acceptRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.acceptRequest(currentUser, requestId);

        return EngagementRequestResponse.from(request);
    }

    @PatchMapping("/api/engagements/{requestId}/decline")
    public EngagementRequestResponse declineRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.declineRequest(currentUser, requestId);

        return EngagementRequestResponse.from(request);
    }

    @PatchMapping("/api/engagements/{requestId}/hold")
    public EngagementRequestResponse holdRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.holdRequest(currentUser, requestId);

        return EngagementRequestResponse.from(request);
    }

    @PatchMapping("/api/engagements/{requestId}/withdraw")
    public EngagementRequestResponse withdrawRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.withdrawRequest(currentUser, requestId);

        return EngagementRequestResponse.from(request);
    }
}