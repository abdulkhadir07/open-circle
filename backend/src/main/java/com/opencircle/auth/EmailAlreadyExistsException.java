package com.opencircle.auth;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailAlreadyExistsException extends ApiException {

    EmailAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
