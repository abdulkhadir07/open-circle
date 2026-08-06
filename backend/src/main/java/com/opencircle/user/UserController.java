package com.opencircle.user;

import com.opencircle.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = authenticatedUserId(jwt);

        // Loads the user represented by the validated JWT subject.
        AppUser user = userService.findById(userId)
                .orElseThrow(CurrentUserNotFoundException::new);

        return UserResponse.from(user);
    }

    private UUID authenticatedUserId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new CurrentUserNotFoundException();
        }
    }
}
