package com.opencircle.security;

import com.opencircle.user.AppUser;
import com.opencircle.user.CurrentUserNotFoundException;
import com.opencircle.user.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Converts Spring Security's JWT principal into the current OpenCircle user.
@Component
public class CurrentUserProvider {

    private final UserService userService;

    CurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    public AppUser getCurrentUser(Jwt jwt) {
        UUID userId = authenticatedUserId(jwt);

        // Loads the database user represented by the validated JWT subject.
        return userService.findById(userId)
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private UUID authenticatedUserId(Jwt jwt) {
        String subject = jwt == null ? null : jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new CurrentUserNotFoundException();
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new CurrentUserNotFoundException();
        }
    }
}