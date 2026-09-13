package com.opencircle.auth;

import java.time.Instant;

record RefreshedAuthentication(
        AccessTokenResponse response,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
