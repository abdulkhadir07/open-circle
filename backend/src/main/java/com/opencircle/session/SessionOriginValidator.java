package com.opencircle.session;

import com.opencircle.security.CorsProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class SessionOriginValidator {

    private final CorsProperties corsProperties;

    SessionOriginValidator(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    public void validate(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !corsProperties.getAllowedOrigins().contains(origin)) {
            throw new InvalidSessionOriginException();
        }
    }
}
