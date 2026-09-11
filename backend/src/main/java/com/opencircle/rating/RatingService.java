package com.opencircle.rating;

import com.opencircle.engagement.EngagementRequest;
import com.opencircle.user.AppUser;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
class RatingService {

    private final RatingEngagementRepository engagements;
    private final RatingObligationRepository obligations;
    private final RatingRepository ratings;
    private final RatingLifecycleService lifecycleService;
    private final EntityManager entityManager;
    private final Clock clock;

    RatingService(
            RatingEngagementRepository engagements,
            RatingObligationRepository obligations,
            RatingRepository ratings,
            RatingLifecycleService lifecycleService,
            EntityManager entityManager,
            Clock clock
    ) {
        this.engagements = engagements;
        this.obligations = obligations;
        this.ratings = ratings;
        this.lifecycleService = lifecycleService;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    // Keeps any deadline reconciliation durable when the request correctly returns a 409 conflict.
    @Transactional(noRollbackFor = RatingNotActionableException.class)
    Rating submitRating(AppUser rater, UUID engagementId, int score) {
        EngagementRequest engagement = engagements.findDetailedById(engagementId)
                .orElseThrow(RatingEngagementNotFoundException::new);

        requireParticipant(engagement, rater);
        Instant now = Instant.now(clock);
        lifecycleService.reconcileEngagement(engagementId, now);
        lifecycleService.finalizeEngagement(engagementId, now);

        List<RatingObligation> pair = obligations.findPairForUpdate(engagementId);
        RatingObligation obligation = pair.stream()
                .filter(candidate -> sameUser(candidate.getRater(), rater))
                .findFirst()
                .orElseThrow(() -> new RatingNotActionableException(
                        "Rating is not available for this engagement"
                ));

        requireSubmittable(obligation);

        UUID obligationId = obligation.getId();
        int claimed = obligations.claimSubmission(obligationId, now);
        if (claimed != 1) {
            throw new RatingNotActionableException("Rating deadline has passed");
        }

        Rating rating = ratings.saveAndFlush(new Rating(
                entityManager.getReference(RatingObligation.class, obligationId),
                score,
                now
        ));
        UUID ratingId = rating.getId();

        ratings.revealResolvedRatings(now);

        return ratings.findDetailedById(ratingId).orElseThrow();
    }

    @Transactional
    List<RatingObligation> getDueRatings(AppUser rater) {
        lifecycleService.reconcileUser(rater.getId());
        return obligations.findDueForRater(rater.getId(), Instant.now(clock));
    }

    @Transactional
    Page<Rating> getReceivedRatings(AppUser ratedUser, int page, int size) {
        requireValidPage(page, size);
        lifecycleService.reconcileUser(ratedUser.getId());

        return ratings.findRevealedReceivedByUserId(
                ratedUser.getId(),
                PageRequest.of(page, size)
        );
    }

    private void requireParticipant(EngagementRequest engagement, AppUser user) {
        if (!sameUser(engagement.getInvitePost().getPoster(), user)
                && !sameUser(engagement.getRequester(), user)) {
            throw new RatingForbiddenException();
        }
    }

    private void requireSubmittable(RatingObligation obligation) {
        switch (obligation.getStatus()) {
            case MONITORING -> throw new RatingNotActionableException("Rating is not required yet");
            case SUBMITTED -> throw new RatingNotActionableException("Rating has already been submitted");
            case MISSED -> throw new RatingNotActionableException("Rating deadline has passed");
            case NOT_REQUIRED -> throw new RatingNotActionableException("Rating is not required for this engagement");
            case REQUIRED -> {
                // The conditional update remains the final authority if the deadline races this check.
            }
        }
    }

    private void requireValidPage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRatingPageException();
        }
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first != null
                && second != null
                && first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
    }
}
