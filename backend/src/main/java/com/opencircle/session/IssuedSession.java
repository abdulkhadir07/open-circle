package com.opencircle.session;

import java.time.Instant;

public record IssuedSession(
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
