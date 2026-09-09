package com.opencircle.invitepost.image;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvitePostImageForbiddenException extends ApiException {

    InvitePostImageForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Only the invite post poster can upload images");
    }
}
