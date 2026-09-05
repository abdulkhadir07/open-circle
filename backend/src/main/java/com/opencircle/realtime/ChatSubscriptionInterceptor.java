package com.opencircle.realtime;

import com.opencircle.chat.ChatRoomService;
import com.opencircle.user.AppUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ChatSubscriptionInterceptor implements ChannelInterceptor {

    private static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chat-rooms/";

    private final ChatRoomService chatRoomService;
    private final WebSocketPrincipalResolver principalResolver;

    ChatSubscriptionInterceptor(
            ChatRoomService chatRoomService,
            WebSocketPrincipalResolver principalResolver
    ) {
        this.chatRoomService = chatRoomService;
        this.principalResolver = principalResolver;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHAT_ROOM_TOPIC_PREFIX)) {
            return message;
        }

        UUID roomId = roomId(destination);
        AppUser user = principalResolver.resolve(accessor.getUser());

        // Only active room participants can subscribe to realtime room updates.
        if (!chatRoomService.isActiveParticipant(user, roomId)) {
            throw new AccessDeniedException("Only active participants can subscribe to this chat room");
        }

        return message;
    }

    private UUID roomId(String destination) {
        try {
            return UUID.fromString(destination.substring(CHAT_ROOM_TOPIC_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new MessagingException("Invalid chat room destination", exception);
        }
    }
}