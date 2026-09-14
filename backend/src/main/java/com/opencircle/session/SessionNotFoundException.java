package com.opencircle.session;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApiException {

    public SessionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Session not found");
    }
}
