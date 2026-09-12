package com.opencircle.score;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Service
class ScoreService {

    private final CircleScoreQueryRepository scores;
    private final Clock clock;

    ScoreService(CircleScoreQueryRepository scores, Clock clock) {
        this.scores = scores;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    CircleScoreSummary getCurrentScore(AppUser user) {
        return scores.findSummary(user.getId(), currentSeasonYear());
    }

    int currentSeasonYear() {
        return Instant.now(clock).atZone(ZoneOffset.UTC).getYear();
    }
}
