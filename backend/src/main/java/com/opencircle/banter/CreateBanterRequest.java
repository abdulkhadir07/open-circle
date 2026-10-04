package com.opencircle.banter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateBanterRequest(
        @NotBlank(message = "Content is required")
        @Size(max = Banter.MAX_CONTENT_LENGTH, message = "Content must not exceed 280 characters")
        String content
) {
}
