package com.opencircle.notification;

import java.util.UUID;

record NotificationCreatedEvent(UUID notificationId, UUID recipientUserId) {
}
