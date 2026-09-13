package com.opencircle.auth;

import com.opencircle.session.IssuedSession;

record AuthenticatedSession(
        AuthResponse response,
        IssuedSession session
) {
}
