package com.opencircle.chat;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidChatAttachmentException extends ApiException {

    InvalidChatAttachmentException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}