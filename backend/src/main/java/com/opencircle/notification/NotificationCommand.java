package com.opencircle.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationCommand(
        UUID recipientUserId,
        UUID actorUserId,
        NotificationType type,
        NotificationResourceType resourceType,
        UUID resourceId,
        NotificationResourceType contextType,
        UUID contextId,
        Instant occurredAt
) {

    public NotificationCommand {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Notification recipient is required");
        }
        if (type == null || resourceType == null || resourceId == null) {
            throw new IllegalArgumentException("Notification type and resource are required");
        }
        if ((contextType == null) != (contextId == null)) {
            throw new IllegalArgumentException("Notification context type and id must both be present or absent");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Notification occurrence time is required");
        }
    }
}
