package com.opencircle.common;

import com.opencircle.mail.MailDeliveryException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MailDeliveryException.class)
    ResponseEntity<ApiError> handleMailDeliveryException(
            MailDeliveryException exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Email delivery failed", exception);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Email service is temporarily unavailable. Please try again later.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception, HttpServletRequest request) {
        return ResponseEntity
                .status(exception.status())
                .body(ApiError.of(exception.status(), exception.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(FieldConflictException.class)
    ResponseEntity<ApiError> handleFieldConflictException(
            FieldConflictException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(exception.status())
                .body(ApiError.validation(
                        exception.status(),
                        exception.getMessage(),
                        request.getRequestURI(),
                        Map.of(exception.field(), exception.getMessage())
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity
                .badRequest()
                .body(ApiError.validation(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }
}
