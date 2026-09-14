package com.opencircle.realtime;

import com.opencircle.chat.ChatRoomPresence;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class RealtimeChatRoomPresence implements ChatRoomPresence {

    private static final String CHAT_ROOM_TOPIC_PREFIX = "/topic/chat-rooms/";

    private final ObjectProvider<SimpUserRegistry> users;

    RealtimeChatRoomPresence(ObjectProvider<SimpUserRegistry> users) {
        this.users = users;
    }

    @Override
    public boolean isPresent(UUID userId, UUID roomId) {
        SimpUser user = users.getObject().getUser(userId.toString());
        if (user == null) {
            return false;
        }

        String destination = CHAT_ROOM_TOPIC_PREFIX + roomId;
        return user.getSessions().stream()
                .flatMap(session -> session.getSubscriptions().stream())
                .map(SimpSubscription::getDestination)
                .anyMatch(destination::equals);
    }
}
