package com.opencircle.invitepost.image;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvitePostImageNotFoundException extends ApiException {

    InvitePostImageNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
