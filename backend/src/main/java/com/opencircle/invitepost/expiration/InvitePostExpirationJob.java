package com.opencircle.invitepost.expiration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.invite-post.expiration-notification-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class InvitePostExpirationJob {

    private final InvitePostExpirationService expirationService;

    InvitePostExpirationJob(InvitePostExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Scheduled(fixedDelayString = "${app.invite-post.expiration-notification-job-delay-ms:60000}")
    void publishExpirationNotifications() {
        expirationService.processDuePosts();
    }
}
