package com.opencircle.invitepost;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

final class InvitePostNotFoundException extends ApiException {

    InvitePostNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Invite post not found");
    }
}
