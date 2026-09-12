CREATE TABLE annual_award_finalizations (
    season_year INTEGER PRIMARY KEY,
    finalized_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_annual_award_finalizations_year
        CHECK (season_year BETWEEN 2000 AND 9999)
);

CREATE TABLE annual_awards (
    id UUID PRIMARY KEY,
    season_year INTEGER NOT NULL,
    winner_user_id UUID NOT NULL,
    final_score BIGINT NOT NULL,
    awarded_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_annual_awards_finalization
        FOREIGN KEY (season_year)
            REFERENCES annual_award_finalizations(season_year)
            ON DELETE RESTRICT,
    CONSTRAINT fk_annual_awards_winner
        FOREIGN KEY (winner_user_id)
            REFERENCES users(id)
            ON DELETE RESTRICT,
    CONSTRAINT uk_annual_awards_season_winner
        UNIQUE (season_year, winner_user_id),
    CONSTRAINT chk_annual_awards_positive_score
        CHECK (final_score > 0)
);

CREATE INDEX idx_annual_awards_winner
    ON annual_awards (winner_user_id, season_year DESC);
