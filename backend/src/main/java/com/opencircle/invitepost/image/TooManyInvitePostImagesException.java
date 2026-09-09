package com.opencircle.invitepost.image;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class TooManyInvitePostImagesException extends ApiException {

    TooManyInvitePostImagesException(int maxImagesPerPost) {
        super(HttpStatus.CONFLICT, "An invite post can have at most " + maxImagesPerPost + " images");
    }
}
