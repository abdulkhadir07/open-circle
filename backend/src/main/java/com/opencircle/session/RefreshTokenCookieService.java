package com.opencircle.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Component
public class RefreshTokenCookieService {

    private final SessionProperties properties;
    private final Clock clock;

    RefreshTokenCookieService(SessionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public ResponseCookie create(String rawToken, Instant expiresAt) {
        Duration maxAge = Duration.between(Instant.now(clock), expiresAt);
        if (maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("Refresh-token cookie must expire in the future");
        }

        return baseCookie(rawToken)
                .maxAge(maxAge)
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path(properties.getCookiePath());
    }
}
