package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EngagementRequestNotFoundException extends ApiException {

    EngagementRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Engagement request not found");
    }
}