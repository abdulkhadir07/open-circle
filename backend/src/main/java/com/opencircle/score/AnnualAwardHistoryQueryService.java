package com.opencircle.score;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnnualAwardHistoryQueryService {

    private final AnnualAwardRepository awards;

    AnnualAwardHistoryQueryService(AnnualAwardRepository awards) {
        this.awards = awards;
    }

    @Transactional(readOnly = true)
    public List<EarnedAnnualAward> findByWinnerUserId(UUID userId) {
        return List.copyOf(awards.findAllForWinner(userId));
    }
}
