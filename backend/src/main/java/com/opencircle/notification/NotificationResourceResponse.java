package com.opencircle.notification;

import java.util.UUID;

public record NotificationResourceResponse(
        NotificationResourceType type,
        UUID id
) {
}
