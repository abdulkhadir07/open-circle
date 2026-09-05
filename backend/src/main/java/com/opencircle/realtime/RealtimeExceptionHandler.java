package com.opencircle.realtime;

import com.opencircle.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
class RealtimeExceptionHandler {

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    RealtimeErrorResponse handleApiException(ApiException exception) {
        return RealtimeErrorResponse.of(exception.status(), exception.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    RealtimeErrorResponse handleValidationException(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );

        return RealtimeErrorResponse.validation(fieldErrors);
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    RealtimeErrorResponse handleAccessDeniedException(AccessDeniedException exception) {
        return RealtimeErrorResponse.of(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @MessageExceptionHandler(MessagingException.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    RealtimeErrorResponse handleMessagingException(MessagingException exception) {
        return RealtimeErrorResponse.of(statusFor(exception), messageFor(exception));
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(value = "/queue/errors", broadcast = false)
    RealtimeErrorResponse handleUnexpectedException() {
        return RealtimeErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Realtime messaging failed"
        );
    }

    private HttpStatus statusFor(MessagingException exception) {
        String message = exception.getMessage();

        if ("Authentication required".equals(message) || "Invalid authentication token".equals(message)) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.BAD_REQUEST;
    }

    private String messageFor(MessagingException exception) {
        return exception.getMessage() == null ? "Realtime message could not be processed" : exception.getMessage();
    }
}