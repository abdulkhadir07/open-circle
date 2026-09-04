package com.opencircle.chat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.chat.auto-close-job-enabled",
        havingValue = "true",
        matchIfMissing = true
)
class ChatRoomAutoCloseJob {

    private final ChatRoomService chatRoomService;

    ChatRoomAutoCloseJob(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }

    @Scheduled(fixedDelayString = "${app.chat.auto-close-job-delay-ms:60000}")
    void closeExpiredRooms() {
        // Closes active, unsaved rooms whose auto-close deadline has passed.
        chatRoomService.closeRoomsReadyForAutoClose();
    }
}