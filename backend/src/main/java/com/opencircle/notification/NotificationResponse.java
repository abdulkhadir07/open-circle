package com.opencircle.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        NotificationActorResponse actor,
        NotificationResourceResponse resource,
        NotificationResourceResponse context,
        int occurrenceCount,
        Instant occurredAt,
        boolean read,
        Instant readAt
) {
}
