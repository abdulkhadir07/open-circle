package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class HiddenChatsPinLockedException extends ApiException {

    HiddenChatsPinLockedException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many incorrect attempts. Try again later");
    }
}
