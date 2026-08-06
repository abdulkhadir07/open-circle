package com.opencircle.verification;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class VerificationAttemptsExceededException extends ApiException {

    VerificationAttemptsExceededException() {
        super(HttpStatus.BAD_REQUEST, "Verification attempts exceeded. Please request a new code");
    }
}
