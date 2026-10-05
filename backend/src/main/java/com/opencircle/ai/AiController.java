package com.opencircle.ai;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final CurrentUserProvider currentUserProvider;
    private final InviteDraftService inviteDraftService;
    private final FeedInsightsService feedInsightsService;

    AiController(
            CurrentUserProvider currentUserProvider,
            InviteDraftService inviteDraftService,
            FeedInsightsService feedInsightsService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.inviteDraftService = inviteDraftService;
        this.feedInsightsService = feedInsightsService;
    }

    @PostMapping("/invite-draft")
    public InviteDraftResponse draftInvite(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InviteDraftRequest request
    ) {
        return inviteDraftService.draft(currentUserProvider.getCurrentUserId(jwt), request.text());
    }

    @GetMapping("/feed-insights")
    public FeedInsightsResponse feedInsights(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return feedInsightsService.insights(currentUser);
    }
}
