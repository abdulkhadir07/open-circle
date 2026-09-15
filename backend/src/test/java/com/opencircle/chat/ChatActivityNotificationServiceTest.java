package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.notification.AggregatedNotificationPublisher;
import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationResourceType;
import com.opencircle.notification.NotificationType;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatActivityNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final ChatRoomParticipantRepository participants = mock(ChatRoomParticipantRepository.class);
    private final ChatRoomPresence presence = mock(ChatRoomPresence.class);
    private final AggregatedNotificationPublisher notifications = mock(AggregatedNotificationPublisher.class);
    private final ChatActivityNotificationService service = new ChatActivityNotificationService(
            participants,
            presence,
            notifications
    );

    @Test
    void publishesOnlyForAwayEligibleRecipients() {
        UUID roomId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID presentRecipientId = UUID.randomUUID();
        UUID awayRecipientId = UUID.randomUUID();
        ChatMessage message = message(roomId, postId, senderId);

        when(participants.findChatActivityRecipientIds(message.getChatRoom(), senderId))
                .thenReturn(List.of(presentRecipientId, awayRecipientId));
        when(presence.isPresent(presentRecipientId, roomId)).thenReturn(true);
        when(presence.isPresent(awayRecipientId, roomId)).thenReturn(false);

        service.notifyAwayParticipants(message);

        ArgumentCaptor<NotificationCommand> command = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notifications).publishAggregated(command.capture());
        assertThat(command.getValue()).isEqualTo(new NotificationCommand(
                awayRecipientId,
                senderId,
                NotificationType.CHAT_ACTIVITY,
                NotificationResourceType.CHAT_ROOM,
                roomId,
                NotificationResourceType.INVITE_POST,
                postId,
                NOW
        ));
        verify(notifications, never()).publishAggregated(new NotificationCommand(
                presentRecipientId,
                senderId,
                NotificationType.CHAT_ACTIVITY,
                NotificationResourceType.CHAT_ROOM,
                roomId,
                NotificationResourceType.INVITE_POST,
                postId,
                NOW
        ));
    }

    @Test
    void doesNothingWhenRepositoryFindsNoEligibleRecipients() {
        ChatMessage message = message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(participants.findChatActivityRecipientIds(
                message.getChatRoom(),
                message.getSender().getId()
        )).thenReturn(List.of());

        service.notifyAwayParticipants(message);

        verifyNoInteractions(presence, notifications);
    }

    private ChatMessage message(UUID roomId, UUID postId, UUID senderId) {
        ChatMessage message = mock(ChatMessage.class);
        ChatRoom room = mock(ChatRoom.class);
        InvitePost post = mock(InvitePost.class);
        AppUser sender = mock(AppUser.class);
        when(message.getChatRoom()).thenReturn(room);
        when(message.getSender()).thenReturn(sender);
        when(message.getCreatedAt()).thenReturn(NOW);
        when(room.getId()).thenReturn(roomId);
        when(room.getInvitePost()).thenReturn(post);
        when(post.getId()).thenReturn(postId);
        when(sender.getId()).thenReturn(senderId);
        return message;
    }
}
