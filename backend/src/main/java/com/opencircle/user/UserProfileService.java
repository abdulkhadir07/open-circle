package com.opencircle.user;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.rating.LifetimeReputationSummary;
import com.opencircle.rating.ReputationSummaryQueryService;
import com.opencircle.score.AnnualAwardHistoryQueryService;
import com.opencircle.score.EarnedAnnualAward;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class UserProfileService {

    private final UserProfileRepository profiles;
    private final UserRepository users;
    private final ProfileImageQueryService profileImages;
    private final ReputationSummaryQueryService reputationSummaries;
    private final AnnualAwardHistoryQueryService awardHistory;
    private final Clock clock;

    UserProfileService(
            UserProfileRepository profiles,
            UserRepository users,
            ProfileImageQueryService profileImages,
            ReputationSummaryQueryService reputationSummaries,
            AnnualAwardHistoryQueryService awardHistory,
            Clock clock
    ) {
        this.profiles = profiles;
        this.users = users;
        this.profileImages = profileImages;
        this.reputationSummaries = reputationSummaries;
        this.awardHistory = awardHistory;
        this.clock = clock;
    }

    @Transactional
    UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = profiles.findForDisplay(userId)
                .orElseThrow(UserProfileNotFoundException::new);
        LifetimeReputationSummary reputation = reputationSummaries.getLifetimeSummary(userId);
        List<EarnedAnnualAward> awards = awardHistory.findByWinnerUserId(userId);
        AppUser user = profile.getUser();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                profile.getDisplayName(),
                profileImages.getProfileImageByUserId(userId),
                profile.getBio(),
                profile.getInterests(),
                user.getCreatedAt(),
                ProfileReputationResponse.from(reputation),
                awards.stream()
                        .map(UserProfileAwardResponse::from)
                        .toList()
        );
    }

    @Transactional
    void replaceProfile(
            UUID userId,
            String username,
            String displayName,
            String bio,
            List<String> interests
    ) {
        UserProfile profile = profiles.findForUpdate(userId)
                .orElseThrow(UserProfileNotFoundException::new);

        AppUser user = profile.getUser();
        String requestedUsername = username == null ? user.getUsername() : username;
        if (!user.getUsername().equalsIgnoreCase(requestedUsername)
                && users.existsByUsernameIgnoreCaseAndIdNot(requestedUsername, userId)) {
            throw new com.opencircle.common.FieldConflictException(
                    "username",
                    "Username is already in use"
            );
        }

        try {
            user.changeUsername(requestedUsername);
            profile.replace(displayName, bio, interests, Instant.now(clock));
        } catch (IllegalArgumentException exception) {
            throw new InvalidUserProfileException(exception.getMessage(), exception);
        }
    }
}
