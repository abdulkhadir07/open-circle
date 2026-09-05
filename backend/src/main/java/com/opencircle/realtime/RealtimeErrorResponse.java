package com.opencircle.realtime;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

record RealtimeErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {

    static RealtimeErrorResponse of(HttpStatus status, String message) {
        return new RealtimeErrorResponse(
                Instant.now(),
                status.value(),
                status.name(),
                message,
                Map.of()
        );
    }

    static RealtimeErrorResponse validation(Map<String, String> fieldErrors) {
        return new RealtimeErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.name(),
                "Validation failed",
                fieldErrors
        );
    }
}