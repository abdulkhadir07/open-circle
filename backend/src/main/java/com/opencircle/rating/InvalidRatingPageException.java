package com.opencircle.rating;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidRatingPageException extends ApiException {

    InvalidRatingPageException() {
        super(HttpStatus.BAD_REQUEST, "Page must be at least 0 and size must be between 1 and 100");
    }
}
