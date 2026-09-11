package com.opencircle.rating;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface RatingObligationRepository extends JpaRepository<RatingObligation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"engagementRequest", "rater", "ratedUser"})
    @Query("""
            select obligation
            from RatingObligation obligation
            where obligation.engagementRequest.id = :engagementId
            order by obligation.id
            """)
    List<RatingObligation> findPairForUpdate(UUID engagementId);

    @EntityGraph(attributePaths = {"engagementRequest", "ratedUser"})
    @Query("""
            select obligation
            from RatingObligation obligation
            where obligation.rater.id = :raterUserId
              and obligation.status = com.opencircle.rating.RatingObligationStatus.REQUIRED
              and obligation.dueAt > :now
            order by obligation.dueAt, obligation.id
            """)
    List<RatingObligation> findDueForRater(UUID raterUserId, Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RatingObligation obligation
            set obligation.status = com.opencircle.rating.RatingObligationStatus.SUBMITTED,
                obligation.submittedAt = :submittedAt,
                obligation.updatedAt = :submittedAt
            where obligation.id = :obligationId
              and obligation.status = com.opencircle.rating.RatingObligationStatus.REQUIRED
              and obligation.dueAt > :submittedAt
            """)
    int claimSubmission(UUID obligationId, Instant submittedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RatingObligation obligation
            set obligation.status = com.opencircle.rating.RatingObligationStatus.MISSED,
                obligation.penaltyPoints = -10,
                obligation.penalizedAt = :now,
                obligation.updatedAt = :now
            where obligation.status = com.opencircle.rating.RatingObligationStatus.REQUIRED
              and obligation.dueAt <= :now
            """)
    int markAllExpiredAsMissed(Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RatingObligation obligation
            set obligation.status = com.opencircle.rating.RatingObligationStatus.MISSED,
                obligation.penaltyPoints = -10,
                obligation.penalizedAt = :now,
                obligation.updatedAt = :now
            where obligation.status = com.opencircle.rating.RatingObligationStatus.REQUIRED
              and obligation.dueAt <= :now
              and obligation.engagementRequest.id = :engagementId
            """)
    int markExpiredAsMissedForEngagement(UUID engagementId, Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RatingObligation obligation
            set obligation.status = com.opencircle.rating.RatingObligationStatus.MISSED,
                obligation.penaltyPoints = -10,
                obligation.penalizedAt = :now,
                obligation.updatedAt = :now
            where obligation.status = com.opencircle.rating.RatingObligationStatus.REQUIRED
              and obligation.dueAt <= :now
              and (obligation.rater.id = :userId or obligation.ratedUser.id = :userId)
            """)
    int markExpiredAsMissedForParticipant(UUID userId, Instant now);
}
