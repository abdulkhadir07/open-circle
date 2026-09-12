package com.opencircle.score;

import java.util.UUID;

record AnnualAwardWinner(
        UUID userId,
        String username,
        long finalScore
) {
}
