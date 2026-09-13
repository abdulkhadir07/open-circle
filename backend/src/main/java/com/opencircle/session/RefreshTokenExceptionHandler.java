package com.opencircle.session;

import com.opencircle.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RefreshTokenExceptionHandler {

    private final RefreshTokenCookieService refreshTokenCookies;

    RefreshTokenExceptionHandler(RefreshTokenCookieService refreshTokenCookies) {
        this.refreshTokenCookies = refreshTokenCookies;
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ApiError> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                Instant.now(),
                exception.status().value(),
                exception.status().name(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );

        return ResponseEntity.status(exception.status())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .body(error);
    }
}
