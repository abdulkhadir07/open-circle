package com.opencircle.score;

import com.opencircle.user.AppUser;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
class AnnualAwardService {

    private final AnnualAwardFinalizationRepository finalizations;
    private final AnnualAwardRepository awards;
    private final CircleScoreQueryRepository scores;
    private final EntityManager entityManager;
    private final Clock clock;

    AnnualAwardService(
            AnnualAwardFinalizationRepository finalizations,
            AnnualAwardRepository awards,
            CircleScoreQueryRepository scores,
            EntityManager entityManager,
            Clock clock
    ) {
        this.finalizations = finalizations;
        this.awards = awards;
        this.scores = scores;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    FinalizedAnnualAward finalizePreviousSeason() {
        return finalizeSeason(currentSeasonYear() - 1);
    }

    @Transactional
    FinalizedAnnualAward finalizeSeason(int seasonYear) {
        if (seasonYear >= currentSeasonYear()) {
            throw new IllegalArgumentException("Only completed seasons can be finalized");
        }

        Instant finalizedAt = Instant.now(clock);
        int claimed = finalizations.claim(seasonYear, finalizedAt);

        if (claimed == 1) {
            AnnualAwardFinalization finalization = finalizations.findById(seasonYear)
                    .orElseThrow(() -> new IllegalStateException("Award finalization claim was not persisted"));
            Instant reputationCutoff = LocalDate.of(seasonYear + 1, 1, 1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

            List<AnnualAward> newAwards = scores
                    .findTopAwardCandidates(seasonYear, reputationCutoff)
                    .stream()
                    .map(candidate -> new AnnualAward(
                            finalization,
                            entityManager.getReference(AppUser.class, candidate.userId()),
                            candidate.annualScore(),
                            finalizedAt
                    ))
                    .toList();

            awards.saveAllAndFlush(newAwards);
        }

        return loadFinalizedAward(seasonYear);
    }

    @Transactional(readOnly = true)
    FinalizedAnnualAward getAward(int seasonYear) {
        return loadFinalizedAward(seasonYear);
    }

    private FinalizedAnnualAward loadFinalizedAward(int seasonYear) {
        AnnualAwardFinalization finalization = finalizations.findById(seasonYear)
                .orElseThrow(() -> new AnnualAwardNotFinalizedException(seasonYear));
        List<AnnualAwardWinner> winners = awards.findAllForSeason(seasonYear).stream()
                .map(award -> new AnnualAwardWinner(
                        award.getWinner().getId(),
                        award.getWinner().getUsername(),
                        award.getFinalScore()
                ))
                .toList();

        return new FinalizedAnnualAward(
                finalization.getSeasonYear(),
                finalization.getFinalizedAt(),
                winners
        );
    }

    private int currentSeasonYear() {
        return Instant.now(clock).atZone(ZoneOffset.UTC).getYear();
    }
}
