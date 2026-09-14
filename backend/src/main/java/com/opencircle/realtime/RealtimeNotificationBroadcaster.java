package com.opencircle.realtime;

import com.opencircle.notification.NotificationBroadcaster;
import com.opencircle.notification.RealtimeNotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class RealtimeNotificationBroadcaster implements NotificationBroadcaster {

    private static final String NOTIFICATION_DESTINATION = "/queue/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    RealtimeNotificationBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(UUID recipientUserId, RealtimeNotificationResponse response) {
        messagingTemplate.convertAndSendToUser(
                recipientUserId.toString(),
                NOTIFICATION_DESTINATION,
                response
        );
    }
}
