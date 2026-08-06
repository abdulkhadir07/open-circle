package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class CurrentUserNotFoundException extends ApiException {

    CurrentUserNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "Authenticated user could not be found");
    }
}
