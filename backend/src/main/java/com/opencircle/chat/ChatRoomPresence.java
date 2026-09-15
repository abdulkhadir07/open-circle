package com.opencircle.chat;

import java.util.UUID;

public interface ChatRoomPresence {

    boolean isPresent(UUID userId, UUID roomId);
}
