package com.opencircle.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateRatingRequest(
        @NotNull(message = "Rating score is required")
        @Min(value = 1, message = "Rating score must be between 1 and 5")
        @Max(value = 5, message = "Rating score must be between 1 and 5")
        Integer score
) {
}
