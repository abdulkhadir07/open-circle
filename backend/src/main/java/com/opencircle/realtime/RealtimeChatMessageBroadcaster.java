package com.opencircle.realtime;

import com.opencircle.chat.ChatMessage;
import com.opencircle.chat.ChatMessageBroadcaster;
import com.opencircle.chat.ChatMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
class RealtimeChatMessageBroadcaster implements ChatMessageBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    RealtimeChatMessageBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(ChatMessage message) {
        ChatMessageResponse response = ChatMessageResponse.from(message);

        messagingTemplate.convertAndSend(
                "/topic/chat-rooms/" + response.roomId(),
                response
        );
    }
}