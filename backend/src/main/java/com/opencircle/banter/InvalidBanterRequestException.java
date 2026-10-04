package com.opencircle.banter;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidBanterRequestException extends ApiException {

    InvalidBanterRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
