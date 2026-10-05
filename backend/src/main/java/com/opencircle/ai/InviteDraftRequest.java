package com.opencircle.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record InviteDraftRequest(
        @NotBlank(message = "Write a sentence about what you want to do first")
        @Size(max = 500, message = "Text must not exceed 500 characters")
        String text
) {
}
