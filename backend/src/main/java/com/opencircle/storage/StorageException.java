package com.opencircle.storage;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;

class StorageException extends ApiException {

    StorageException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}