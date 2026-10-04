package com.opencircle.banter;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/banter")
public class BanterController {

    private final CurrentUserProvider currentUserProvider;
    private final BanterService banterService;
    private final ProfileImageQueryService profileImageQueryService;

    BanterController(
            CurrentUserProvider currentUserProvider,
            BanterService banterService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.banterService = banterService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping
    public BanterPageResponse getBoard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "new") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        BanterBoard board = banterService.getBoard(currentUser, sort, page, size);

        Set<UUID> authorIds = new HashSet<>();
        board.items().forEach(view -> authorIds.add(view.banter().getAuthor().getId()));
        Map<UUID, ProfileImageResponse> images = profileImageQueryService.getProfileImagesByUserIds(authorIds);

        List<BanterResponse> items = board.items().stream()
                .map(view -> BanterResponse.from(view, images.get(view.banter().getAuthor().getId()), currentUser.getId()))
                .toList();

        return new BanterPageResponse(items, board.page(), board.size(), board.totalElements(), board.totalPages());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BanterResponse createBanter(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBanterRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        BanterView view = banterService.create(currentUser, request.content());

        return BanterResponse.from(
                view,
                profileImageQueryService.getProfileImageByUserId(currentUser.getId()),
                currentUser.getId()
        );
    }

    @DeleteMapping("/{banterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID banterId) {
        banterService.delete(currentUserProvider.getCurrentUser(jwt), banterId);
    }

    @PutMapping("/{banterId}/like")
    public BanterLikeResponse likeBanter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID banterId) {
        return banterService.like(currentUserProvider.getCurrentUser(jwt), banterId);
    }

    @DeleteMapping("/{banterId}/like")
    public BanterLikeResponse unlikeBanter(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID banterId) {
        return banterService.unlike(currentUserProvider.getCurrentUser(jwt), banterId);
    }

    @GetMapping("/{banterId}/replies")
    public List<BanterReplyResponse> getReplies(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID banterId) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        List<BanterReply> replies = banterService.getReplies(currentUser, banterId);

        Set<UUID> authorIds = new HashSet<>();
        replies.forEach(reply -> authorIds.add(reply.getAuthor().getId()));
        Map<UUID, ProfileImageResponse> images = profileImageQueryService.getProfileImagesByUserIds(authorIds);

        return replies.stream()
                .map(reply -> BanterReplyResponse.from(reply, images.get(reply.getAuthor().getId()), currentUser.getId()))
                .toList();
    }

    @PostMapping("/{banterId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public BanterReplyResponse createReply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID banterId,
            @Valid @RequestBody CreateBanterRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        BanterReply reply = banterService.reply(currentUser, banterId, request.content());

        return BanterReplyResponse.from(
                reply,
                profileImageQueryService.getProfileImageByUserId(currentUser.getId()),
                currentUser.getId()
        );
    }

    @DeleteMapping("/{banterId}/replies/{replyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID banterId,
            @PathVariable UUID replyId
    ) {
        banterService.deleteReply(currentUserProvider.getCurrentUser(jwt), banterId, replyId);
    }
}
