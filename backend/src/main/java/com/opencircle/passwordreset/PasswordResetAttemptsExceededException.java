package com.opencircle.passwordreset;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class PasswordResetAttemptsExceededException extends ApiException {

    PasswordResetAttemptsExceededException() {
        super(HttpStatus.BAD_REQUEST, "Password reset attempts exceeded. Please request a new code");
    }
}
