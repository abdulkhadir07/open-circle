package com.opencircle.rating;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class RatingForbiddenException extends ApiException {

    RatingForbiddenException() {
        super(HttpStatus.FORBIDDEN, "Only engagement participants can rate this interaction");
    }
}
