package com.opencircle.accountsettings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record SetHiddenChatsPinRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4 to 6 digits")
        String pin
) {
}
