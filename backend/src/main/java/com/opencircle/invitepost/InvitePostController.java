package com.opencircle.invitepost;

import com.opencircle.invitepost.image.InvitePostImageResponse;
import com.opencircle.invitepost.image.InvitePostImageService;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invite-posts")
public class InvitePostController {

    private final CurrentUserProvider currentUserProvider;
    private final InvitePostService invitePostService;
    private final InvitePostImageService imageService;
    private final ProfileImageQueryService profileImageQueryService;

    InvitePostController(
            CurrentUserProvider currentUserProvider,
            InvitePostService invitePostService,
            InvitePostImageService imageService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.invitePostService = invitePostService;
        this.imageService = imageService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitePostResponse createPost(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInvitePostRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        InvitePost post = invitePostService.createPost(currentUser, request);

        return InvitePostResponse.from(
                post,
                profileImageQueryService.getProfileImageByUserId(post.getPoster().getId()),
                List.of()
        );
    }

    @GetMapping("/local")
    public List<InvitePostResponse> getLocalFeed(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocationScope scope
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return responsesFor(invitePostService.getLocalFeed(currentUser, scope));
    }

    @GetMapping("/global")
    public List<InvitePostResponse> getGlobalFeed(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return responsesFor(invitePostService.getGlobalFeed(currentUser));
    }

    private List<InvitePostResponse> responsesFor(List<InvitePost> posts) {
        Map<UUID, List<InvitePostImageResponse>> imagesByPost = imageService.getImageResponses(posts);
        Set<UUID> posterIds = posts.stream()
                .map(post -> post.getPoster().getId())
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImagesByUser =
                profileImageQueryService.getProfileImagesByUserIds(posterIds);

        return posts.stream()
                .map(post -> InvitePostResponse.from(
                        post,
                        profileImagesByUser.get(post.getPoster().getId()),
                        imagesByPost.getOrDefault(post.getId(), List.of())
                ))
                .toList();
    }
}
