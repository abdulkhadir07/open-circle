package com.opencircle.accountsettings;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class NewPasswordMatchesCurrentException extends ApiException {

    NewPasswordMatchesCurrentException() {
        super(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
    }
}
