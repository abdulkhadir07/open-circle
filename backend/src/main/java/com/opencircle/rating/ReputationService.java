package com.opencircle.rating;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class ReputationService {

    private final UserService users;
    private final RatingLifecycleService lifecycleService;
    private final RatingContributionQueryService contributions;
    private final ProfileImageQueryService profileImages;

    ReputationService(
            UserService users,
            RatingLifecycleService lifecycleService,
            RatingContributionQueryService contributions,
            ProfileImageQueryService profileImages
    ) {
        this.users = users;
        this.lifecycleService = lifecycleService;
        this.contributions = contributions;
        this.profileImages = profileImages;
    }

    @Transactional
    ReputationResponse getReputation(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(ReputationUserNotFoundException::new);

        lifecycleService.reconcileUser(userId);
        LifetimeReputationSummary summary = contributions.getLifetimeSummary(userId);

        return new ReputationResponse(
                user.getId(),
                user.getUsername(),
                profileImages.getProfileImageByUserId(userId),
                summary.averageRating(),
                summary.totalRatingsReceived(),
                summary.distinctRaterCount()
        );
    }
}
