package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ChatRoomActionNotAllowedException extends ApiException {

    ChatRoomActionNotAllowedException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}