package com.opencircle.chat;

import com.opencircle.notification.AggregatedNotificationPublisher;
import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationResourceType;
import com.opencircle.notification.NotificationType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class ChatActivityNotificationService {

    private final ChatRoomParticipantRepository participants;
    private final ChatRoomPresence presence;
    private final AggregatedNotificationPublisher notifications;

    ChatActivityNotificationService(
            ChatRoomParticipantRepository participants,
            ChatRoomPresence presence,
            AggregatedNotificationPublisher notifications
    ) {
        this.participants = participants;
        this.presence = presence;
        this.notifications = notifications;
    }

    void notifyAwayParticipants(ChatMessage message) {
        ChatRoom room = message.getChatRoom();
        UUID senderUserId = message.getSender().getId();

        participants.findChatActivityRecipientIds(room, senderUserId).stream()
                .filter(recipientUserId -> !presence.isPresent(recipientUserId, room.getId()))
                .forEach(recipientUserId -> notifications.publishAggregated(new NotificationCommand(
                        recipientUserId,
                        senderUserId,
                        NotificationType.CHAT_ACTIVITY,
                        NotificationResourceType.CHAT_ROOM,
                        room.getId(),
                        NotificationResourceType.INVITE_POST,
                        room.getInvitePost().getId(),
                        message.getCreatedAt()
                )));
    }
}
