package com.opencircle.score;

import java.time.Instant;
import java.util.List;

record FinalizedAnnualAward(
        int seasonYear,
        Instant finalizedAt,
        List<AnnualAwardWinner> winners
) {
    FinalizedAnnualAward {
        winners = List.copyOf(winners);
    }
}
