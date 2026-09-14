package com.opencircle.accountsettings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record VerifyEmailChangeRequest(
        @NotBlank(message = "Email change code is required")
        @Pattern(regexp = "\\d{6}", message = "Email change code must be 6 digits")
        String code
) {
}
