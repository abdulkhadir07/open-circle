package com.opencircle.ai;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

// Content stopped by Safety Guardian. The message is shown to the person, so it explains what to change.
public class UnsafeContentException extends ApiException {

    public UnsafeContentException(String reason) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }
}
