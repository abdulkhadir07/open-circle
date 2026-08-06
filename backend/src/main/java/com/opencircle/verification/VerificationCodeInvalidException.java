package com.opencircle.verification;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class VerificationCodeInvalidException extends ApiException {

    VerificationCodeInvalidException() {
        super(HttpStatus.BAD_REQUEST, "Invalid or expired verification code");
    }
}
