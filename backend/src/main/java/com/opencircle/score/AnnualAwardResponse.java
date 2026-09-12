package com.opencircle.score;

import java.time.Instant;
import java.util.List;

public record AnnualAwardResponse(
        int seasonYear,
        String name,
        Instant finalizedAt,
        List<AnnualAwardWinnerResponse> winners
) {
    public AnnualAwardResponse {
        winners = List.copyOf(winners);
    }
}
