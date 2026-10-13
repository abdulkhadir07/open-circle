package com.opencircle.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

record UpdateUserProfileRequest(
        @Size(
                min = AppUser.MIN_USERNAME_LENGTH,
                max = AppUser.MAX_USERNAME_LENGTH,
                message = "Username must be between 3 and 30 characters"
        )
        @Pattern(
                regexp = AppUser.USERNAME_REGEX,
                message = "Username can only contain letters, numbers, periods, and underscores"
        )
        String username,

        @NotBlank(message = "Display name is required")
        @Size(max = UserProfile.MAX_DISPLAY_NAME_LENGTH)
        String displayName,

        @Size(max = UserProfile.MAX_BIO_LENGTH)
        String bio,

        @NotNull(message = "Interests are required; use an empty list for none")
        @Size(max = UserProfile.MAX_INTERESTS, message = "A profile can have at most 8 interests")
        List<@NotBlank(message = "Interests cannot be blank")
                @Size(max = UserProfile.MAX_INTEREST_LENGTH) String> interests
) {
}
