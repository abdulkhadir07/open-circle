package com.opencircle.rating;

import java.math.BigDecimal;

record LifetimeReputationSummary(
        BigDecimal averageRating,
        long totalRatingsReceived,
        long distinctRaterCount
) {
}
