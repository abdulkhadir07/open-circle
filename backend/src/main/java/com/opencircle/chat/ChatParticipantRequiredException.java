package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ChatParticipantRequiredException extends ApiException {

    ChatParticipantRequiredException() {
        super(HttpStatus.FORBIDDEN, "You must be a chat participant to perform this action");
    }
}