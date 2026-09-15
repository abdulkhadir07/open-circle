package com.opencircle.notification;

import java.util.UUID;

record NotificationDeliveryRequestedEvent(UUID notificationId, UUID recipientUserId) {
}
