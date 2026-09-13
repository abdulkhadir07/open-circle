package com.opencircle.rating;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReputationSummaryQueryService {

    private final RatingLifecycleService lifecycleService;
    private final RatingContributionQueryService contributions;

    ReputationSummaryQueryService(
            RatingLifecycleService lifecycleService,
            RatingContributionQueryService contributions
    ) {
        this.lifecycleService = lifecycleService;
        this.contributions = contributions;
    }

    @Transactional
    public LifetimeReputationSummary getLifetimeSummary(UUID userId) {
        lifecycleService.reconcileUser(userId);
        return contributions.getLifetimeSummary(userId);
    }
}
