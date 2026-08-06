package com.opencircle.auth;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class InvalidCredentialsException extends ApiException {

    InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
