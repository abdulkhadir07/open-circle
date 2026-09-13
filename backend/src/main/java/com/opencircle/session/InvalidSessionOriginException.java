package com.opencircle.session;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidSessionOriginException extends ApiException {

    InvalidSessionOriginException() {
        super(HttpStatus.FORBIDDEN, "Session request origin is not allowed");
    }
}
