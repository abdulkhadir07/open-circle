package com.opencircle.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

record SignupRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 80)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80)
        String lastName,

        @Email(message = "Please enter a valid email address")
        @NotBlank(message = "Email is required")
        @Size(max = 160)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be at least 8 characters!")
        String password,

        @NotBlank(message = "Phone Number is required")
        @Size(max = 30)
        String phoneNumber,

        @Past
        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotBlank(message = "City is required")
        @Size(max = 80)
        String city,

        @NotBlank
        @Size(max = 80)
        String stateRegion,

        @NotBlank(message = "Country is required")
        @Size(max = 80)
        String country
) {
}
