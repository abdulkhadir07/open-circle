package com.opencircle.rating;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class RatingEngagementNotFoundException extends ApiException {

    RatingEngagementNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Engagement not found");
    }
}
