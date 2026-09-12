package com.opencircle.user;

import com.opencircle.rating.LifetimeReputationSummary;

import java.math.BigDecimal;

public record ProfileReputationResponse(
        BigDecimal averageRating,
        long totalRatingsReceived,
        long distinctRaterCount
) {

    static ProfileReputationResponse from(LifetimeReputationSummary summary) {
        return new ProfileReputationResponse(
                summary.averageRating(),
                summary.totalRatingsReceived(),
                summary.distinctRaterCount()
        );
    }
}
