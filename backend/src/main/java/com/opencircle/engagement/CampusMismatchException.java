package com.opencircle.engagement;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class CampusMismatchException extends ApiException {

    CampusMismatchException() {
        super(HttpStatus.FORBIDDEN, "This invite is only open to people on the poster's campus");
    }
}
