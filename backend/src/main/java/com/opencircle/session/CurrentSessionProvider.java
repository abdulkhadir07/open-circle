package com.opencircle.session;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentSessionProvider {

    public UUID getCurrentSessionId(Jwt jwt) {
        String sessionId = jwt == null ? null : jwt.getClaimAsString("sid");
        if (sessionId == null || sessionId.isBlank()) {
            throw new CurrentSessionUnavailableException();
        }

        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException exception) {
            throw new CurrentSessionUnavailableException();
        }
    }
}
