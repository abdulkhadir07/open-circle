package com.opencircle.score;

import java.util.List;

record CurrentScoreboard(int seasonYear, List<RankedScoreboardEntry> entries) {

    CurrentScoreboard {
        entries = List.copyOf(entries);
    }
}
