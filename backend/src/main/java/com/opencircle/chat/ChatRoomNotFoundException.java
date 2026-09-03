package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ChatRoomNotFoundException extends ApiException {

    ChatRoomNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Chat room not found");
    }
}