package com.opencircle.realtime;

import java.security.Principal;
import java.util.UUID;

class WebSocketUserPrincipal implements Principal {

    private final UUID userId;

    WebSocketUserPrincipal(UUID userId) {
        this.userId = userId;
    }

    UUID userId() {
        return userId;
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}