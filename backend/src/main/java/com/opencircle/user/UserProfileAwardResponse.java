package com.opencircle.user;

import com.opencircle.score.EarnedAnnualAward;

import java.time.Instant;

public record UserProfileAwardResponse(
        int seasonYear,
        String name,
        long finalScore,
        Instant awardedAt
) {

    static UserProfileAwardResponse from(EarnedAnnualAward award) {
        return new UserProfileAwardResponse(
                award.seasonYear(),
                "Circle Champion " + award.seasonYear(),
                award.finalScore(),
                award.awardedAt()
        );
    }
}
