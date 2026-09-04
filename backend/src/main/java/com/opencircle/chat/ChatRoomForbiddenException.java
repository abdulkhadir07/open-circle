package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ChatRoomForbiddenException extends ApiException {

    ChatRoomForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}