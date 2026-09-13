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
    private final ReputationSummaryQueryService reputationSummaries;
    private final ProfileImageQueryService profileImages;

    ReputationService(
            UserService users,
            ReputationSummaryQueryService reputationSummaries,
            ProfileImageQueryService profileImages
    ) {
        this.users = users;
        this.reputationSummaries = reputationSummaries;
        this.profileImages = profileImages;
    }

    @Transactional
    ReputationResponse getReputation(UUID userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(ReputationUserNotFoundException::new);

        LifetimeReputationSummary summary = reputationSummaries.getLifetimeSummary(userId);

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
