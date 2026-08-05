package com.opencircle.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record ResendVerificationRequest(

        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        String email
) {
}
