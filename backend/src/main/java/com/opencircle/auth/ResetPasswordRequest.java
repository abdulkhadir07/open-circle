package com.opencircle.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record ResetPasswordRequest(
        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Password reset code is required")
        @Pattern(regexp = "\\d{6}", message = "Password reset code must be 6 digits")
        String code,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String newPassword
) {
}
