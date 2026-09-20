package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class HiddenChatsPinNotSetException extends ApiException {

    HiddenChatsPinNotSetException() {
        super(HttpStatus.BAD_REQUEST, "No hidden chats PIN has been set");
    }
}
