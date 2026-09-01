package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class DuplicateEngagementRequestException extends ApiException {

    DuplicateEngagementRequestException() {
        super(HttpStatus.CONFLICT, "You have already requested to engage with this post");
    }
}