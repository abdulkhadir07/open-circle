package com.opencircle.score;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

interface AnnualAwardFinalizationRepository
        extends JpaRepository<AnnualAwardFinalization, Integer> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO annual_award_finalizations (season_year, finalized_at)
            VALUES (:seasonYear, :finalizedAt)
            ON CONFLICT (season_year) DO NOTHING
            """, nativeQuery = true)
    int claim(int seasonYear, Instant finalizedAt);
}
