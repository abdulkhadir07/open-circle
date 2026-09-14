package com.opencircle.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.notification.cleanup-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class NotificationCleanupJob {

    private final NotificationService notificationService;

    NotificationCleanupJob(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${app.notification.cleanup-cron:0 15 3 * * *}", zone = "UTC")
    void cleanup() {
        notificationService.deleteExpired();
    }
}
