package com.opencircle.session;

import java.time.Instant;
import java.util.UUID;

public record RefreshedSession(
        UUID userId,
        UUID sessionId,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
