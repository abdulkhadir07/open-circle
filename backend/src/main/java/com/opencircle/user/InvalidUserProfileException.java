package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidUserProfileException extends ApiException {

    InvalidUserProfileException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, message, cause);
    }
}
