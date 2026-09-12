package com.opencircle.score;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ScoreboardService {

    private static final int MAXIMUM_PUBLIC_RANK = 5;

    private final CircleScoreQueryRepository scores;
    private final ScoreService scoreService;

    ScoreboardService(CircleScoreQueryRepository scores, ScoreService scoreService) {
        this.scores = scores;
        this.scoreService = scoreService;
    }

    @Transactional(readOnly = true)
    CurrentScoreboard getCurrentScoreboard() {
        int seasonYear = scoreService.currentSeasonYear();
        return new CurrentScoreboard(
                seasonYear,
                scores.findTopRanks(seasonYear, MAXIMUM_PUBLIC_RANK)
        );
    }
}
