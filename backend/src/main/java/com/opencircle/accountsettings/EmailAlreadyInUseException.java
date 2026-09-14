package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailAlreadyInUseException extends ApiException {

    EmailAlreadyInUseException() {
        super(HttpStatus.CONFLICT, "Email is already registered");
    }
}
