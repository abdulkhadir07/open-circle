package com.opencircle.rating;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
class RatingRevealService {

    private final RatingRevealRepository ratings;
    private final RatingNotificationService notifications;
    private final EntityManager entityManager;

    RatingRevealService(
            RatingRevealRepository ratings,
            RatingNotificationService notifications,
            EntityManager entityManager
    ) {
        this.ratings = ratings;
        this.notifications = notifications;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    int revealResolvedRatings(Instant revealedAt) {
        List<RevealedRating> revealedRatings = ratings.revealResolved(revealedAt);
        entityManager.clear();
        notifications.notifyRevealed(revealedRatings);
        return revealedRatings.size();
    }
}
