package com.opencircle.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void fieldConflictReturnsAConflictWithTheNamedFieldError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/me/profile");

        var response = handler.handleFieldConflictException(
                new FieldConflictException("username", "Username is already in use"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path()).isEqualTo("/api/users/me/profile");
        assertThat(response.getBody().fieldErrors())
                .containsEntry("username", "Username is already in use");
    }
}
