package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailChangeAttemptsExceededException extends ApiException {

    EmailChangeAttemptsExceededException() {
        super(HttpStatus.BAD_REQUEST, "Email change attempts exceeded. Please request a new code");
    }
}
