package com.opencircle.score;

import com.opencircle.profileimage.ProfileImageResponse;

import java.util.UUID;

public record AnnualAwardWinnerResponse(
        UUID userId,
        String username,
        ProfileImageResponse profileImage,
        long finalScore
) {

    static AnnualAwardWinnerResponse from(
            AnnualAwardWinner winner,
            ProfileImageResponse profileImage
    ) {
        return new AnnualAwardWinnerResponse(
                winner.userId(),
                winner.username(),
                profileImage,
                winner.finalScore()
        );
    }
}
