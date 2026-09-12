package com.opencircle.score;

import java.util.List;

public record ScoreboardResponse(
        int seasonYear,
        List<ScoreboardEntryResponse> entries
) {

    public ScoreboardResponse {
        entries = List.copyOf(entries);
    }
}
