package com.opencircle.user;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageQueryService profileImageQueryService;

    UserController(
            CurrentUserProvider currentUserProvider,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // Returns the profile for the user represented by the validated bearer token.
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        return UserResponse.from(
                currentUser,
                profileImageQueryService.getProfileImageByUserId(currentUser.getId())
        );
    }
}
