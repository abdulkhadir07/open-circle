package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class EmailUnchangedException extends ApiException {

    EmailUnchangedException() {
        super(HttpStatus.BAD_REQUEST, "New email must be different from the current email");
    }
}
