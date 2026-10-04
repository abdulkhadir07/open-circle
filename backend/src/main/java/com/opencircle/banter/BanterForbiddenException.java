package com.opencircle.banter;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class BanterForbiddenException extends ApiException {

    BanterForbiddenException() {
        super(HttpStatus.FORBIDDEN, "You can only delete your own banter");
    }
}
