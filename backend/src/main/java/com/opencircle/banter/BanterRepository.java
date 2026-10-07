package com.opencircle.banter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

interface BanterRepository extends JpaRepository<Banter, UUID> {

    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select banter
                    from Banter banter
                    order by banter.createdAt desc, banter.id desc
                    """,
            countQuery = "select count(banter) from Banter banter"
    )
    Page<Banter> findNewest(Pageable pageable);

    // "Hot" weighs a reply as two likes; ties fall back to newest so paging stays stable.
    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select banter
                    from Banter banter
                    order by
                        ((select count(bl) from BanterLike bl where bl.banter = banter)
                            + 2 * (select count(reply) from BanterReply reply where reply.banter = banter)) desc,
                        banter.createdAt desc,
                        banter.id desc
                    """,
            countQuery = "select count(banter) from Banter banter"
    )
    Page<Banter> findHot(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Query("select banter from Banter banter where banter.id = :id")
    Optional<Banter> findWithAuthor(UUID id);
}
