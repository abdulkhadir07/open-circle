package com.opencircle.session;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class CurrentSessionUnavailableException extends ApiException {

    public CurrentSessionUnavailableException() {
        super(HttpStatus.UNAUTHORIZED, "Current session is unavailable; please sign in again");
    }
}
