package com.opencircle.rating;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class ReputationUserNotFoundException extends ApiException {

    ReputationUserNotFoundException() {
        super(HttpStatus.NOT_FOUND, "User not found");
    }
}
