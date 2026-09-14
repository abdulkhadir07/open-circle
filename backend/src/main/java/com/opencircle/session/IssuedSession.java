package com.opencircle.session;

import java.time.Instant;
import java.util.UUID;

public record IssuedSession(
        UUID sessionId,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
