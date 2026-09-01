package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EngagementRequestRepository extends JpaRepository<EngagementRequest, UUID> {

    boolean existsByInvitePostAndRequester(InvitePost invitePost, AppUser requester);

    Optional<EngagementRequest> findByInvitePostAndRequester(InvitePost invitePost, AppUser requester);

    List<EngagementRequest> findByInvitePostOrderByCreatedAtDesc(InvitePost invitePost);

    // Loads the request, invite post, poster, and requester together for ownership checks.
    @Query("""
            select request
            from EngagementRequest request
            join fetch request.invitePost post
            join fetch post.poster
            join fetch request.requester
            where request.id = :id
            """)
    Optional<EngagementRequest> findDetailedById(UUID id);
}