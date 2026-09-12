package com.opencircle.score;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.awards.finalization-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class AnnualAwardJob {

    private final AnnualAwardService awardService;

    AnnualAwardJob(AnnualAwardService awardService) {
        this.awardService = awardService;
    }

    @Scheduled(
            cron = "${app.awards.finalization-job-cron:0 5 0 * * *}",
            zone = "UTC"
    )
    void finalizePreviousSeason() {
        awardService.finalizePreviousSeason();
    }
}
