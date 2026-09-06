package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ChatAttachmentNotFoundException extends ApiException {

    ChatAttachmentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}