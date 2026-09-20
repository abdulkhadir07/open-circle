package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class HiddenChatsPinIncorrectException extends ApiException {

    HiddenChatsPinIncorrectException() {
        super(HttpStatus.UNAUTHORIZED, "Incorrect PIN");
    }
}
