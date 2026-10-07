package com.opencircle.banter;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class BanterNotFoundException extends ApiException {

    BanterNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Banter not found");
    }
}
