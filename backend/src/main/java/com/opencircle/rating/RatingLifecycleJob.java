package com.opencircle.rating;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.rating.lifecycle-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class RatingLifecycleJob {

    private final RatingLifecycleService lifecycleService;

    RatingLifecycleJob(RatingLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedDelayString = "${app.rating.lifecycle-job-delay-ms:60000}")
    void processRatingLifecycle() {
        lifecycleService.runLifecycle();
    }
}
