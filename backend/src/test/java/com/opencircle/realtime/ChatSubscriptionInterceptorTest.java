package com.opencircle.realtime;

import com.opencircle.chat.ChatRoomService;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatSubscriptionInterceptorTest {

    private final ChatRoomService chatRoomService = mock(ChatRoomService.class);
    private final WebSocketPrincipalResolver principalResolver = mock(WebSocketPrincipalResolver.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(
            chatRoomService,
            principalResolver
    );

    @Test
    void activeParticipantCanSubscribeToChatRoomTopic() {
        UUID roomId = UUID.randomUUID();
        Principal principal = new WebSocketUserPrincipal(UUID.randomUUID());
        AppUser user = user("subscriber@example.com");
        Message<?> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat-rooms/" + roomId, principal);

        when(principalResolver.resolve(principal)).thenReturn(user);
        when(chatRoomService.isActiveParticipant(user, roomId)).thenReturn(true);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
    }

    @Test
    void nonParticipantCannotSubscribeToChatRoomTopic() {
        UUID roomId = UUID.randomUUID();
        Principal principal = new WebSocketUserPrincipal(UUID.randomUUID());
        AppUser user = user("outsider@example.com");
        Message<?> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat-rooms/" + roomId, principal);

        when(principalResolver.resolve(principal)).thenReturn(user);
        when(chatRoomService.isActiveParticipant(user, roomId)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only active participants can subscribe to this chat room");
    }

    @Test
    void invalidChatRoomTopicThrowsMessagingException() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/chat-rooms/not-a-uuid",
                new WebSocketUserPrincipal(UUID.randomUUID())
        );

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Invalid chat room destination");

        verifyNoInteractions(principalResolver, chatRoomService);
    }

    @Test
    void nonChatRoomTopicPassesThroughWithoutAuthorizationCheck() {
        Message<?> message = stompMessage(
                StompCommand.SUBSCRIBE,
                "/topic/other",
                new WebSocketUserPrincipal(UUID.randomUUID())
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);

        verifyNoInteractions(principalResolver, chatRoomService);
    }

    @Test
    void nonSubscribeFramePassesThroughWithoutAuthorizationCheck() {
        Message<?> message = stompMessage(
                StompCommand.SEND,
                "/topic/chat-rooms/" + UUID.randomUUID(),
                new WebSocketUserPrincipal(UUID.randomUUID())
        );

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);

        verifyNoInteractions(principalResolver, chatRoomService);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        accessor.setDestination(destination);
        accessor.setUser(principal);

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private AppUser user(String email) {
        return new AppUser(
                "test_" + Math.abs(email.hashCode()),
                "Test",
                "User",
                email,
                "hashed-password",
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}