package com.opencircle.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationDeliveryListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryListener.class);

    private final NotificationQueryService notificationQueryService;
    private final NotificationBroadcaster broadcaster;

    NotificationDeliveryListener(
            NotificationQueryService notificationQueryService,
            NotificationBroadcaster broadcaster
    ) {
        this.notificationQueryService = notificationQueryService;
        this.broadcaster = broadcaster;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void deliver(NotificationCreatedEvent event) {
        try {
            notificationQueryService.getRealtimeResponse(
                            event.notificationId(),
                            event.recipientUserId()
                    )
                    .ifPresent(response -> broadcaster.broadcast(event.recipientUserId(), response));
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to deliver notification {} to user {}",
                    event.notificationId(),
                    event.recipientUserId(),
                    exception
            );
        }
    }
}
