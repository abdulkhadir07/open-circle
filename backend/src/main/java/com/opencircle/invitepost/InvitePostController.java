package com.opencircle.invitepost;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invite-posts")
public class InvitePostController {

    private final CurrentUserProvider currentUserProvider;
    private final InvitePostService invitePostService;

    InvitePostController(
            CurrentUserProvider currentUserProvider,
            InvitePostService invitePostService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.invitePostService = invitePostService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitePostResponse createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInvitePostRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        InvitePost post = invitePostService.createPost(currentUser, request);

        return InvitePostResponse.from(post);
    }

    @GetMapping("/local")
    public List<InvitePostResponse> getLocalFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocationScope scope
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return invitePostService.getLocalFeed(currentUser, scope)
                .stream()
                .map(InvitePostResponse::from)
                .toList();
    }

    @GetMapping("/global")
    public List<InvitePostResponse> getGlobalFeed(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return invitePostService.getGlobalFeed(currentUser)
                .stream()
                .map(InvitePostResponse::from)
                .toList();
    }
}