package com.opencircle.banter;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface BanterReplyRepository extends JpaRepository<BanterReply, UUID> {

    @EntityGraph(attributePaths = "author")
    @Query("""
            select reply
            from BanterReply reply
            where reply.banter.id = :banterId
            order by reply.createdAt asc, reply.id asc
            """)
    List<BanterReply> findThread(UUID banterId);

    @EntityGraph(attributePaths = "author")
    @Query("select reply from BanterReply reply where reply.id = :replyId and reply.banter.id = :banterId")
    Optional<BanterReply> findInBanter(UUID replyId, UUID banterId);

    @Query("""
            select reply.banter.id as banterId, count(reply) as total
            from BanterReply reply
            where reply.banter.id in :banterIds
            group by reply.banter.id
            """)
    List<BanterCount> countByBanterIds(Collection<UUID> banterIds);

    long countByBanterId(UUID banterId);
}
