package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class HiddenChatsPinConfirmationException extends ApiException {

    HiddenChatsPinConfirmationException() {
        super(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    }
}
