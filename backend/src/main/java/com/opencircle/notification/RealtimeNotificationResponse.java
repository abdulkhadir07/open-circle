package com.opencircle.notification;

public record RealtimeNotificationResponse(
        NotificationResponse notification,
        long unreadCount
) {
}
