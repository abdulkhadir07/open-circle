package com.opencircle.session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.session.cleanup-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class SessionCleanupJob {

    private final SessionService sessionService;

    SessionCleanupJob(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(cron = "${app.session.cleanup-cron:0 15 3 * * *}", zone = "UTC")
    void cleanup() {
        sessionService.cleanupExpiredAndRevoked();
    }
}
