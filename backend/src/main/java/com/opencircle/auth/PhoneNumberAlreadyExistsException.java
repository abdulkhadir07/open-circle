package com.opencircle.auth;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class PhoneNumberAlreadyExistsException extends ApiException {

    PhoneNumberAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "Phone number is already registered");
    }
}
