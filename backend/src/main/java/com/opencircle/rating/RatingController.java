package com.opencircle.rating;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RatingController {

    private final CurrentUserProvider currentUserProvider;
    private final RatingService ratingService;
    private final ReputationService reputationService;
    private final ProfileImageQueryService profileImageQueryService;

    RatingController(
            CurrentUserProvider currentUserProvider,
            RatingService ratingService,
            ReputationService reputationService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.ratingService = ratingService;
        this.reputationService = reputationService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping("/users/me/ratings/due")
    public List<DueRatingResponse> getDueRatings(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        List<RatingObligation> dueRatings = ratingService.getDueRatings(currentUser);
        Set<UUID> otherUserIds = dueRatings.stream()
                .map(obligation -> obligation.getRatedUser().getId())
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImages =
                profileImageQueryService.getProfileImagesByUserIds(otherUserIds);

        return dueRatings.stream()
                .map(obligation -> DueRatingResponse.from(
                        obligation,
                        profileImages.get(obligation.getRatedUser().getId())
                ))
                .toList();
    }

    @PostMapping("/engagements/{engagementId}/ratings")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingSubmissionResponse submitRating(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID engagementId,
            @Valid @RequestBody CreateRatingRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        return RatingSubmissionResponse.from(
                ratingService.submitRating(currentUser, engagementId, request.score())
        );
    }

    @GetMapping("/users/me/ratings/received")
    public ReceivedRatingsResponse getReceivedRatings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        Page<Rating> receivedPage = ratingService.getReceivedRatings(currentUser, page, size);
        Set<UUID> raterUserIds = receivedPage.getContent().stream()
                .map(rating -> rating.getObligation().getRater().getId())
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImages =
                profileImageQueryService.getProfileImagesByUserIds(raterUserIds);

        List<ReceivedRatingResponse> responses = receivedPage.getContent().stream()
                .map(rating -> ReceivedRatingResponse.from(
                        rating,
                        profileImages.get(rating.getObligation().getRater().getId())
                ))
                .toList();

        return new ReceivedRatingsResponse(
                responses,
                receivedPage.getNumber(),
                receivedPage.getSize(),
                receivedPage.getTotalElements(),
                receivedPage.getTotalPages()
        );
    }

    @GetMapping("/users/{userId}/reputation")
    public ReputationResponse getReputation(@PathVariable UUID userId) {
        return reputationService.getReputation(userId);
    }
}
