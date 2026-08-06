package com.opencircle.auth;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailNotVerifiedException extends ApiException {

    EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "Please verify your email before logging in");
    }
}
