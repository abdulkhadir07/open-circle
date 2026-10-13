package com.opencircle.accountsettings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record DeleteAccountRequest(
        @NotBlank(message = "Current password is required")
        @Size(max = 72, message = "Current password must not exceed 72 characters")
        String currentPassword,

        @NotBlank(message = "Type DELETE to confirm account deletion")
        @Pattern(regexp = "DELETE", message = "Type DELETE to confirm account deletion")
        String confirmation
) {
}
