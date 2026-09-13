package com.opencircle.session;

import com.opencircle.security.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionOriginValidatorTest {

    private final SessionOriginValidator validator = new SessionOriginValidator(properties());

    @Test
    void acceptsConfiguredBrowserOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUntrustedBrowserOrigin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.ORIGIN, "https://attacker.example");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(InvalidSessionOriginException.class)
                .hasMessage("Session request origin is not allowed");
    }

    @Test
    void acceptsNonBrowserClientWithoutOriginHeader() {
        assertThatCode(() -> validator.validate(new MockHttpServletRequest()))
                .doesNotThrowAnyException();
    }

    private CorsProperties properties() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("http://localhost:5173"));
        return properties;
    }
}
