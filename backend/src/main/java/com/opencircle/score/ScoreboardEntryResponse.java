package com.opencircle.score;

import com.opencircle.profileimage.ProfileImageResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record ScoreboardEntryResponse(
        long rank,
        UUID userId,
        String username,
        ProfileImageResponse profileImage,
        long annualScore,
        BigDecimal averageRating,
        long currentYearDistinctRaterCount
) {

    static ScoreboardEntryResponse from(
            RankedScoreboardEntry entry,
            ProfileImageResponse profileImage
    ) {
        return new ScoreboardEntryResponse(
                entry.rank(),
                entry.userId(),
                entry.username(),
                profileImage,
                entry.annualScore(),
                entry.averageRating(),
                entry.currentYearDistinctRaterCount()
        );
    }
}
