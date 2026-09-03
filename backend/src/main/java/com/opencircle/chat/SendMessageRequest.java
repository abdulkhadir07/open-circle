package com.opencircle.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message body is required")
        @Size(max = 1000, message = "Message cannot be longer than 1000 characters")
        String body
) {
}