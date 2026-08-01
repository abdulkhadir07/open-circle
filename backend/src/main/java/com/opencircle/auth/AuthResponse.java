package com.opencircle.auth;

import com.opencircle.user.dto.UserResponse;

record AuthResponse(
        String token,
        UserResponse user
) {
}
