package com.opencircle.score;

import java.math.BigDecimal;
import java.util.UUID;

record RankedScoreboardEntry(
        long rank,
        UUID userId,
        String username,
        long annualScore,
        BigDecimal averageRating,
        long currentYearDistinctRaterCount
) {
}
