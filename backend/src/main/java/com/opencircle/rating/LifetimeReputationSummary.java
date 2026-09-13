package com.opencircle.rating;

import java.math.BigDecimal;

public record LifetimeReputationSummary(
        BigDecimal averageRating,
        long totalRatingsReceived,
        long distinctRaterCount
) {
}
