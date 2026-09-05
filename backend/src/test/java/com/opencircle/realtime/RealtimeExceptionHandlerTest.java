package com.opencircle.realtime;

import com.opencircle.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessagingException;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeExceptionHandlerTest {

    private final RealtimeExceptionHandler handler = new RealtimeExceptionHandler();

    @Test
    void apiExceptionUsesApplicationStatusAndMessage() {
        RealtimeErrorResponse response = handler.handleApiException(new TestApiException());

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.error()).isEqualTo("CONFLICT");
        assertThat(response.message()).isEqualTo("Already exists");
        assertThat(response.fieldErrors()).isEmpty();
    }

    @Test
    void accessDeniedExceptionReturnsForbiddenError() {
        RealtimeErrorResponse response = handler.handleAccessDeniedException(
                new AccessDeniedException("Forbidden")
        );

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.error()).isEqualTo("FORBIDDEN");
        assertThat(response.message()).isEqualTo("Forbidden");
    }

    @Test
    void authenticationMessagingExceptionReturnsUnauthorizedError() {
        RealtimeErrorResponse response = handler.handleMessagingException(
                new MessagingException("Authentication required")
        );

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.error()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).isEqualTo("Authentication required");
    }

    @Test
    void genericMessagingExceptionReturnsBadRequestError() {
        RealtimeErrorResponse response = handler.handleMessagingException(
                new MessagingException("Invalid chat room destination")
        );

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.error()).isEqualTo("BAD_REQUEST");
        assertThat(response.message()).isEqualTo("Invalid chat room destination");
    }

    @Test
    void unexpectedExceptionReturnsGenericServerError() {
        RealtimeErrorResponse response = handler.handleUnexpectedException();

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.error()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.message()).isEqualTo("Realtime messaging failed");
    }

    private static class TestApiException extends ApiException {

        TestApiException() {
            super(HttpStatus.CONFLICT, "Already exists");
        }
    }
}