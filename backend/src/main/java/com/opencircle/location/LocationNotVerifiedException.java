package com.opencircle.location;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

public class LocationNotVerifiedException extends ApiException {

    public LocationNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "Please verify your location before using this feature");
    }
}