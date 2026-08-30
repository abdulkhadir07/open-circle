package com.opencircle.invitepost;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidInvitePostRequestException extends ApiException {

    InvalidInvitePostRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}