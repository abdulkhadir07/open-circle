package com.opencircle.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
class PersistentAggregatedNotificationPublisher implements AggregatedNotificationPublisher {

    private final AggregatedNotificationRepository notifications;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PersistentAggregatedNotificationPublisher(
            AggregatedNotificationRepository notifications,
            ApplicationEventPublisher events,
            Clock clock
    ) {
        this.notifications = notifications;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAggregated(NotificationCommand command) {
        UUID notificationId = notifications.upsert(
                UUID.randomUUID(),
                command,
                Instant.now(clock)
        );

        events.publishEvent(new NotificationDeliveryRequestedEvent(notificationId, command.recipientUserId()));
    }
}
