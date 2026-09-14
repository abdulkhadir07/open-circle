package com.opencircle.notification;

import java.util.UUID;

public interface NotificationBroadcaster {

    void broadcast(UUID recipientUserId, RealtimeNotificationResponse response);
}
