package com.opencircle.user;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserProvider currentUserProvider;
    private final ProfileImageQueryService profileImageQueryService;
    private final UserProfileService userProfileService;

    UserController(
            CurrentUserProvider currentUserProvider,
            ProfileImageQueryService profileImageQueryService,
            UserProfileService userProfileService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.profileImageQueryService = profileImageQueryService;
        this.userProfileService = userProfileService;
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

    @GetMapping("/{userId}/profile")
    public UserProfileResponse getProfile(@PathVariable UUID userId) {
        return userProfileService.getProfile(userId);
    }

    @GetMapping("/me/profile")
    public UserProfileResponse getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getProfile(
                currentUserProvider.getCurrentUser(jwt).getId()
        );
    }

    @PutMapping("/me/profile")
    public UserProfileResponse replaceCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUser(jwt).getId();
        userProfileService.replaceProfile(
                userId,
                request.displayName(),
                request.bio(),
                request.interests()
        );
        return userProfileService.getProfile(userId);
    }
}
