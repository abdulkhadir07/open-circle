package com.opencircle.location;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import com.opencircle.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/location")
public class LocationController {

    private final CurrentUserProvider currentUserProvider;
    private final LocationVerificationService locationVerificationService;
    private final ProfileImageQueryService profileImageQueryService;

    LocationController(
            CurrentUserProvider currentUserProvider,
            LocationVerificationService locationVerificationService,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.locationVerificationService = locationVerificationService;
        this.profileImageQueryService = profileImageQueryService;
    }

    @PutMapping
    public UserResponse verifyMyLocation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VerifyLocationRequest request
    ) {
        AppUser user = currentUserProvider.getCurrentUser(jwt);

        // Resolves and stores the verified location for the authenticated user.
        AppUser updatedUser = locationVerificationService.verifyLocation(user, request);

        return UserResponse.from(
                updatedUser,
                profileImageQueryService.getProfileImageByUserId(updatedUser.getId())
        );
    }
}
