package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailChangeCodeInvalidException extends ApiException {

    EmailChangeCodeInvalidException() {
        super(HttpStatus.BAD_REQUEST, "Invalid or expired email change code");
    }
}
