package com.opencircle.score;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface AnnualAwardRepository extends JpaRepository<AnnualAward, UUID> {

    @EntityGraph(attributePaths = "winner")
    @Query("""
            select award
            from AnnualAward award
            where award.finalization.seasonYear = :seasonYear
            order by award.winner.id
            """)
    List<AnnualAward> findAllForSeason(int seasonYear);
}
