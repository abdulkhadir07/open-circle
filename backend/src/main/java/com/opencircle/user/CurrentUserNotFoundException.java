package com.opencircle.user;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class CurrentUserNotFoundException extends ApiException {

    public CurrentUserNotFoundException() {
        super(HttpStatus.UNAUTHORIZED, "Authenticated user could not be found");
    }
}
