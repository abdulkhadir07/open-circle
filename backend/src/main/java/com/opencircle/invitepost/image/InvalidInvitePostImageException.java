package com.opencircle.invitepost.image;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidInvitePostImageException extends ApiException {

    InvalidInvitePostImageException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
