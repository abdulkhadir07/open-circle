package com.opencircle.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
class NotificationService {

    private final NotificationRepository notifications;
    private final NotificationProperties properties;
    private final Clock clock;

    NotificationService(
            NotificationRepository notifications,
            NotificationProperties properties,
            Clock clock
    ) {
        this.notifications = notifications;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    void markRead(UUID recipientUserId, UUID notificationId) {
        int matched = notifications.markRead(
                notificationId,
                recipientUserId,
                Instant.now(clock)
        );

        if (matched == 0) {
            throw new NotificationNotFoundException();
        }
    }

    @Transactional
    void markAllRead(UUID recipientUserId) {
        notifications.markAllRead(recipientUserId, Instant.now(clock));
    }

    @Transactional
    int deleteExpired() {
        Instant cutoff = Instant.now(clock)
                .minus(properties.getRetentionDays(), ChronoUnit.DAYS);
        return notifications.deleteOccurredBefore(cutoff);
    }
}
