package com.opencircle.realtime;

import com.opencircle.chat.ChatMessage;
import com.opencircle.chat.ChatMessageResponse;
import com.opencircle.chat.ChatRoomService;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
class RealtimeChatController {

    private final ChatRoomService chatRoomService;
    private final WebSocketPrincipalResolver principalResolver;
    private final SimpMessagingTemplate messagingTemplate;

    RealtimeChatController(
            ChatRoomService chatRoomService,
            WebSocketPrincipalResolver principalResolver,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatRoomService = chatRoomService;
        this.principalResolver = principalResolver;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat-rooms/{roomId}/messages")
    void sendMessage(
            Principal principal,
            @DestinationVariable UUID roomId,
            @Valid @Payload RealtimeSendMessageRequest request
    ) {
        AppUser sender = principalResolver.resolve(principal);

        // Reuses the REST message path so realtime messages keep the same room rules and persistence behavior.
        ChatMessage message = chatRoomService.sendMessage(sender, roomId, request.body());

        messagingTemplate.convertAndSend(
                "/topic/chat-rooms/" + roomId,
                ChatMessageResponse.from(message)
        );
    }
}