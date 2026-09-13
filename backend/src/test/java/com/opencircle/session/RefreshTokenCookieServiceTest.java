package com.opencircle.session;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookieServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");

    private final SessionProperties properties = properties();
    private final RefreshTokenCookieService cookies = new RefreshTokenCookieService(
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createsHostOnlyHttpOnlySecureSameSiteCookie() {
        ResponseCookie cookie = cookies.create("raw-token", NOW.plusSeconds(3600));

        assertThat(cookie.getName()).isEqualTo("open_circle_refresh");
        assertThat(cookie.getValue()).isEqualTo("raw-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getMaxAge()).hasSeconds(3600);
    }

    @Test
    void readsOnlyTheConfiguredRefreshCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("unrelated", "value"),
                new Cookie("open_circle_refresh", "raw-token")
        );

        assertThat(cookies.read(request)).contains("raw-token");
    }

    @Test
    void clearCookieUsesTheSameSecurityScopeAndZeroMaxAge() {
        ResponseCookie cookie = cookies.clear();

        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    private SessionProperties properties() {
        SessionProperties properties = new SessionProperties();
        properties.setCookieSecure(true);
        return properties;
    }
}
