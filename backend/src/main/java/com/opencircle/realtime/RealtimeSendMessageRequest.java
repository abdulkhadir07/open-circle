package com.opencircle.realtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RealtimeSendMessageRequest(
        @NotBlank(message = "Message body is required")
        @Size(max = 1000, message = "Message cannot be longer than 1000 characters")
        String body
) {
}