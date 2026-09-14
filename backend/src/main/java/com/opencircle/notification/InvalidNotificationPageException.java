package com.opencircle.notification;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidNotificationPageException extends ApiException {

    InvalidNotificationPageException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
