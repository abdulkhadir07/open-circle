package com.opencircle.realtime;

import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
class WebSocketPrincipalResolver {

    private final UserService userService;

    WebSocketPrincipalResolver(UserService userService) {
        this.userService = userService;
    }

    AppUser resolve(Principal principal) {
        if (!(principal instanceof WebSocketUserPrincipal webSocketUser)) {
            throw new MessagingException("Authentication required");
        }

        return userService.findById(webSocketUser.userId())
                .orElseThrow(() -> new MessagingException("Authentication required"));
    }
}