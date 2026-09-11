package com.opencircle.rating;

import com.opencircle.engagement.EngagementRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Optional;
import java.util.UUID;

interface RatingEngagementRepository extends Repository<EngagementRequest, UUID> {

    @Query("""
            select engagement
            from EngagementRequest engagement
            join fetch engagement.invitePost post
            join fetch post.poster
            join fetch engagement.requester
            where engagement.id = :engagementId
            """)
    Optional<EngagementRequest> findDetailedById(UUID engagementId);
}
