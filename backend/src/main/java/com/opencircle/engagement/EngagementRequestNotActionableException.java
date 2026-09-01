package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EngagementRequestNotActionableException extends ApiException {

    EngagementRequestNotActionableException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}