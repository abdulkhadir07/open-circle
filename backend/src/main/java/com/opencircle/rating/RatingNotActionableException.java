package com.opencircle.rating;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class RatingNotActionableException extends ApiException {

    RatingNotActionableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
