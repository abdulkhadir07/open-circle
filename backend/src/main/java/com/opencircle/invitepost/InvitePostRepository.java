package com.opencircle.invitepost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface InvitePostRepository extends JpaRepository<InvitePost, UUID> {

    @Query("""
            select post
            from InvitePost post
            where post.status = :status
              and post.expiresAt > :now
              and post.locationScope = :locationScope
              and post.country = :country
            order by post.createdAt desc
            """)
    List<InvitePost> findCountryFeed(
            InvitePostStatus status,
            Instant now,
            LocationScope locationScope,
            String country
    );

    @Query("""
            select post
            from InvitePost post
            where post.status = :status
              and post.expiresAt > :now
              and post.locationScope = :locationScope
              and post.country = :country
              and post.stateRegion = :stateRegion
            order by post.createdAt desc
            """)
    List<InvitePost> findStateRegionFeed(
            InvitePostStatus status,
            Instant now,
            LocationScope locationScope,
            String country,
            String stateRegion
    );

    @Query("""
            select post
            from InvitePost post
            where post.status = :status
              and post.expiresAt > :now
              and post.locationScope = :locationScope
              and post.country = :country
              and post.city = :city
            order by post.createdAt desc
            """)
    List<InvitePost> findCityFeed(
            InvitePostStatus status,
            Instant now,
            LocationScope locationScope,
            String country,
            String city
    );

    @Query("""
            select post
            from InvitePost post
            where post.status = :status
              and post.expiresAt > :now
              and post.locationScope = :locationScope
            order by post.createdAt desc
            """)
    List<InvitePost> findGlobalFeed(
            InvitePostStatus status,
            Instant now,
            LocationScope locationScope
    );
}