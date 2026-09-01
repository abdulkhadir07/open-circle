package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class CannotEngageOwnPostException extends ApiException {

    CannotEngageOwnPostException() {
        super(HttpStatus.BAD_REQUEST, "You cannot engage with your own invite post");
    }
}