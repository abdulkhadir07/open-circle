package com.opencircle.common;

import org.springframework.http.HttpStatus;

public final class FieldConflictException extends ApiException {

    private final String field;

    public FieldConflictException(String field, String message) {
        super(HttpStatus.CONFLICT, message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
