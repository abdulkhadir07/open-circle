package com.opencircle.location;
import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class LocationResolutionException extends ApiException {

    LocationResolutionException() {
        super(HttpStatus.BAD_REQUEST, "Unable to verify location from coordinates");
    }
}
