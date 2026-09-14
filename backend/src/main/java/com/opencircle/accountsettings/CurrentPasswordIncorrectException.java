package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class CurrentPasswordIncorrectException extends ApiException {

    CurrentPasswordIncorrectException() {
        super(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    }
}
