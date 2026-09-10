package com.opencircle.realtime;

import com.opencircle.chat.ChatMessage;
import com.opencircle.chat.ChatMessageBroadcaster;
import com.opencircle.chat.ChatMessageResponse;
import com.opencircle.chat.ChatRoomService;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
class RealtimeChatController {

    private final ChatRoomService chatRoomService;
    private final WebSocketPrincipalResolver principalResolver;
    private final ChatMessageBroadcaster messageBroadcaster;
    private final ProfileImageQueryService profileImageQueryService;

    RealtimeChatController(
            ChatRoomService chatRoomService,
            WebSocketPrincipalResolver principalResolver,
            ChatMessageBroadcaster messageBroadcaster,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.chatRoomService = chatRoomService;
        this.principalResolver = principalResolver;
        this.messageBroadcaster = messageBroadcaster;
        this.profileImageQueryService = profileImageQueryService;
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
        ChatMessageResponse response = ChatMessageResponse.from(
                message,
                profileImageQueryService.getProfileImageByUserId(sender.getId())
        );

        messageBroadcaster.broadcast(response);
    }
}
