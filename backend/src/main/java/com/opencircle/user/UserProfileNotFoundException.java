package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class UserProfileNotFoundException extends ApiException {

    UserProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "User profile not found");
    }
}
