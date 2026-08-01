package com.opencircle.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(
        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password
) {
}
