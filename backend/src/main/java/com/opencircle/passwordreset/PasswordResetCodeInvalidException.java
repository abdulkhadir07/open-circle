package com.opencircle.passwordreset;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class PasswordResetCodeInvalidException extends ApiException {

    PasswordResetCodeInvalidException() {
        super(HttpStatus.BAD_REQUEST, "Invalid or expired password reset code");
    }
}
