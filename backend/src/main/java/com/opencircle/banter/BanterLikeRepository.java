package com.opencircle.banter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

interface BanterLikeRepository extends JpaRepository<BanterLike, UUID> {

    @Query("""
            select bl.banter.id as banterId, count(bl) as total
            from BanterLike bl
            where bl.banter.id in :banterIds
            group by bl.banter.id
            """)
    List<BanterCount> countByBanterIds(Collection<UUID> banterIds);

    @Query("select bl.banter.id from BanterLike bl where bl.user.id = :userId and bl.banter.id in :banterIds")
    Set<UUID> findLikedBanterIds(UUID userId, Collection<UUID> banterIds);

    long countByBanterId(UUID banterId);

    // Liking twice (or two taps racing) is a no-op rather than an error.
    @Modifying
    @Query(
            value = """
                    insert into banter_likes (id, banter_id, user_id, created_at)
                    values (:id, :banterId, :userId, :createdAt)
                    on conflict (banter_id, user_id) do nothing
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("banterId") UUID banterId,
            @Param("userId") UUID userId,
            @Param("createdAt") Instant createdAt
    );

    @Modifying
    @Query("delete from BanterLike bl where bl.banter.id = :banterId and bl.user.id = :userId")
    int deleteByBanterIdAndUserId(UUID banterId, UUID userId);
}
