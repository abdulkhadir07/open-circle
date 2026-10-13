package com.opencircle.invitepost;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

final class InvitePostForbiddenException extends ApiException {

    InvitePostForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Only the post owner can change this invite");
    }
}
