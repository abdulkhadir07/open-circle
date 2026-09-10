package com.opencircle.engagement;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping
public class EngagementRequestController {

    private final CurrentUserProvider currentUserProvider;
    private final EngagementRequestService engagementRequestService;
    private final ProfileImageQueryService profileImageQueryService;

    EngagementRequestController(
            CurrentUserProvider currentUserProvider,
            EngagementRequestService engagementRequestService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.engagementRequestService = engagementRequestService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @PostMapping("/api/invite-posts/{postId}/engagements")
    @ResponseStatus(HttpStatus.CREATED)
    public EngagementRequestResponse createRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.createRequest(currentUser, postId);

        return responseFor(request);
    }

    @GetMapping("/api/invite-posts/{postId}/engagements")
    public List<EngagementRequestResponse> getRequestsForPost(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID postId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return responsesFor(engagementRequestService.getRequestsForPost(currentUser, postId));
    }

    @PatchMapping("/api/engagements/{requestId}/accept")
    public EngagementRequestResponse acceptRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.acceptRequest(currentUser, requestId);

        return responseFor(request);
    }

    @PatchMapping("/api/engagements/{requestId}/decline")
    public EngagementRequestResponse declineRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.declineRequest(currentUser, requestId);

        return responseFor(request);
    }

    @PatchMapping("/api/engagements/{requestId}/hold")
    public EngagementRequestResponse holdRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.holdRequest(currentUser, requestId);

        return responseFor(request);
    }

    @PatchMapping("/api/engagements/{requestId}/withdraw")
    public EngagementRequestResponse withdrawRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID requestId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        EngagementRequest request = engagementRequestService.withdrawRequest(currentUser, requestId);

        return responseFor(request);
    }

    private EngagementRequestResponse responseFor(EngagementRequest request) {
        return EngagementRequestResponse.from(
                request,
                profileImageQueryService.getProfileImageByUserId(request.getRequester().getId())
        );
    }

    private List<EngagementRequestResponse> responsesFor(List<EngagementRequest> requests) {
        Set<UUID> requesterIds = requests.stream()
                .map(request -> request.getRequester().getId())
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImagesByUser =
                profileImageQueryService.getProfileImagesByUserIds(requesterIds);

        return requests.stream()
                .map(request -> EngagementRequestResponse.from(
                        request,
                        profileImagesByUser.get(request.getRequester().getId())
                ))
                .toList();
    }
}
