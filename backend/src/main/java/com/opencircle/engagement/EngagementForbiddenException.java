package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EngagementForbiddenException extends ApiException {

    EngagementForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}