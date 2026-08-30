package com.opencircle.location;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class LocationServiceUnavailableException extends ApiException {

    LocationServiceUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Location verification is temporarily unavailable, please try again");
    }
}
