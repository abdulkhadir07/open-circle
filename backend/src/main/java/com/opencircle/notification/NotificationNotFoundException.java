package com.opencircle.notification;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class NotificationNotFoundException extends ApiException {

    NotificationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Notification not found");
    }
}
