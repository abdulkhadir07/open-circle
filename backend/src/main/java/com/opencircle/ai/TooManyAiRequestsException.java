package com.opencircle.ai;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class TooManyAiRequestsException extends ApiException {

    public TooManyAiRequestsException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "You're doing that a lot. Please wait a moment and try again.");
    }
}
