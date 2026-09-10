package com.opencircle.profileimage;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidProfileImageException extends ApiException {

    InvalidProfileImageException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
