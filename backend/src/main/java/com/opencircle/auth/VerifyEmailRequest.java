package com.opencircle.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record VerifyEmailRequest(

        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
        String code
) {
}
