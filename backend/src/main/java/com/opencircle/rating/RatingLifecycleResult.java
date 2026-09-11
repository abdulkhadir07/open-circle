package com.opencircle.rating;

record RatingLifecycleResult(
        int activatedEngagements,
        int missedObligations,
        int revealedRatings
) {
}
