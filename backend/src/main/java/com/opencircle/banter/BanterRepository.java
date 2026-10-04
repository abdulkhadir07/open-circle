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
                    where banter.campus = :campus
                    order by banter.createdAt desc, banter.id desc
                    """,
            countQuery = "select count(banter) from Banter banter where banter.campus = :campus"
    )
    Page<Banter> findNewest(String campus, Pageable pageable);

    // "Hot" weighs a reply as two likes; ties fall back to newest so paging stays stable.
    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select banter
                    from Banter banter
                    where banter.campus = :campus
                    order by
                        ((select count(bl) from BanterLike bl where bl.banter = banter)
                            + 2 * (select count(reply) from BanterReply reply where reply.banter = banter)) desc,
                        banter.createdAt desc,
                        banter.id desc
                    """,
            countQuery = "select count(banter) from Banter banter where banter.campus = :campus"
    )
    Page<Banter> findHot(String campus, Pageable pageable);

    // Banters from another campus are treated as if they don't exist.
    @EntityGraph(attributePaths = "author")
    @Query("select banter from Banter banter where banter.id = :id and banter.campus = :campus")
    Optional<Banter> findInCampus(UUID id, String campus);
}
