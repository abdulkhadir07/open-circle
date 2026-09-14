package com.opencircle.accountsettings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record StartEmailChangeRequest(
        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "New email is required")
        @Size(max = 160)
        String newEmail,

        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must not exceed 72 characters")
        String currentPassword
) {
}
