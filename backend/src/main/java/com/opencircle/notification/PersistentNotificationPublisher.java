package com.opencircle.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
class PersistentNotificationPublisher implements NotificationPublisher {

    private final NotificationRepository notifications;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PersistentNotificationPublisher(
            NotificationRepository notifications,
            ApplicationEventPublisher events,
            Clock clock
    ) {
        this.notifications = notifications;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(NotificationCommand command) {
        UUID notificationId = UUID.randomUUID();
        Instant createdAt = Instant.now(clock);

        int inserted = notifications.insertIfAbsent(
                notificationId,
                command.recipientUserId(),
                command.actorUserId(),
                command.type().name(),
                command.resourceType().name(),
                command.resourceId(),
                command.contextType() == null ? null : command.contextType().name(),
                command.contextId(),
                command.occurredAt(),
                createdAt
        );

        if (inserted == 1) {
            events.publishEvent(new NotificationCreatedEvent(notificationId, command.recipientUserId()));
        }
    }
}
