package com.opencircle.session;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }
}
