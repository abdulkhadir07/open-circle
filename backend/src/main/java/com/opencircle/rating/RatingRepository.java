package com.opencircle.rating;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface RatingRepository extends JpaRepository<Rating, UUID> {

    @Query("""
            select rating
            from Rating rating
            join fetch rating.obligation obligation
            join fetch obligation.rater
            join fetch obligation.ratedUser
            where rating.id = :ratingId
            """)
    Optional<Rating> findDetailedById(UUID ratingId);

    @Query(
            value = """
                    select rating
                    from Rating rating
                    join fetch rating.obligation obligation
                    join fetch obligation.rater
                    where obligation.ratedUser.id = :ratedUserId
                      and rating.revealedAt is not null
                    order by rating.revealedAt desc, rating.id desc
                    """,
            countQuery = """
                    select count(rating)
                    from Rating rating
                    where rating.obligation.ratedUser.id = :ratedUserId
                      and rating.revealedAt is not null
                    """
    )
    Page<Rating> findRevealedReceivedByUserId(UUID ratedUserId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ratings rating
            SET revealed_at = :revealedAt
            FROM rating_obligations obligation
            WHERE obligation.id = rating.obligation_id
              AND rating.revealed_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM rating_obligations unresolved
                  WHERE unresolved.engagement_request_id = obligation.engagement_request_id
                    AND unresolved.status IN ('MONITORING', 'REQUIRED')
              )
            """, nativeQuery = true)
    int revealResolvedRatings(Instant revealedAt);
}
