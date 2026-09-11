CREATE TABLE rating_obligations (
    id UUID PRIMARY KEY,
    engagement_request_id UUID NOT NULL,
    rater_user_id UUID NOT NULL,
    rated_user_id UUID NOT NULL,

    status VARCHAR(20) NOT NULL,
    requirement_trigger VARCHAR(30),
    required_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    penalty_points INTEGER,
    penalized_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_rating_obligations_engagement
        FOREIGN KEY (engagement_request_id)
            REFERENCES engagement_requests(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_rating_obligations_rater
        FOREIGN KEY (rater_user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_rating_obligations_rated
        FOREIGN KEY (rated_user_id)
            REFERENCES users(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_rating_obligations_engagement_rater
        UNIQUE (engagement_request_id, rater_user_id),
    CONSTRAINT chk_rating_obligations_direction
        CHECK (rater_user_id <> rated_user_id),
    CONSTRAINT chk_rating_obligations_status
        CHECK (status IN ('MONITORING', 'REQUIRED', 'SUBMITTED', 'MISSED', 'NOT_REQUIRED')),
    CONSTRAINT chk_rating_obligations_trigger
        CHECK (
            requirement_trigger IS NULL
            OR requirement_trigger IN ('PARTICIPANT_EXIT', 'CHAT_INACTIVITY', 'MAX_DURATION')
        ),
    CONSTRAINT chk_rating_obligations_state
        CHECK (
            (
                status IN ('MONITORING', 'NOT_REQUIRED')
                AND requirement_trigger IS NULL
                AND required_at IS NULL
                AND due_at IS NULL
                AND submitted_at IS NULL
                AND penalty_points IS NULL
                AND penalized_at IS NULL
            )
            OR
            (
                status = 'REQUIRED'
                AND requirement_trigger IS NOT NULL
                AND required_at IS NOT NULL
                AND due_at IS NOT NULL
                AND submitted_at IS NULL
                AND penalty_points IS NULL
                AND penalized_at IS NULL
            )
            OR
            (
                status = 'SUBMITTED'
                AND requirement_trigger IS NOT NULL
                AND required_at IS NOT NULL
                AND due_at IS NOT NULL
                AND submitted_at IS NOT NULL
                AND penalty_points IS NULL
                AND penalized_at IS NULL
            )
            OR
            (
                status = 'MISSED'
                AND requirement_trigger IS NOT NULL
                AND required_at IS NOT NULL
                AND due_at IS NOT NULL
                AND submitted_at IS NULL
                AND penalty_points = -10
                AND penalized_at IS NOT NULL
            )
        ),
    CONSTRAINT chk_rating_obligations_deadline
        CHECK (due_at IS NULL OR due_at = required_at + INTERVAL '24 hours'),
    CONSTRAINT chk_rating_obligations_submission_time
        CHECK (submitted_at IS NULL OR (submitted_at >= required_at AND submitted_at < due_at)),
    CONSTRAINT chk_rating_obligations_penalty_time
        CHECK (penalized_at IS NULL OR penalized_at >= due_at),
    CONSTRAINT chk_rating_obligations_timestamps
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_rating_obligations_monitoring
    ON rating_obligations (engagement_request_id)
    WHERE status = 'MONITORING';

CREATE INDEX idx_rating_obligations_due
    ON rating_obligations (due_at)
    WHERE status = 'REQUIRED';

CREATE INDEX idx_rating_obligations_rater_due
    ON rating_obligations (rater_user_id, due_at)
    WHERE status = 'REQUIRED';

CREATE INDEX idx_rating_obligations_rater
    ON rating_obligations (rater_user_id);

CREATE INDEX idx_rating_obligations_rated
    ON rating_obligations (rated_user_id);

CREATE TABLE ratings (
    id UUID PRIMARY KEY,
    obligation_id UUID NOT NULL,
    score INTEGER NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    revealed_at TIMESTAMPTZ,

    CONSTRAINT fk_ratings_obligation
        FOREIGN KEY (obligation_id)
            REFERENCES rating_obligations(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_ratings_obligation
        UNIQUE (obligation_id),
    CONSTRAINT chk_ratings_score
        CHECK (score BETWEEN 1 AND 5),
    CONSTRAINT chk_ratings_reveal_time
        CHECK (revealed_at IS NULL OR revealed_at >= submitted_at)
);

CREATE INDEX idx_ratings_revealed
    ON ratings (revealed_at DESC)
    WHERE revealed_at IS NOT NULL;

CREATE VIEW rating_contributions AS
SELECT
    rating_id,
    engagement_request_id,
    rater_user_id,
    rated_user_id,
    score,
    revealed_at,
    season_year,
    ROW_NUMBER() OVER (
        PARTITION BY rater_user_id, rated_user_id
        ORDER BY revealed_at DESC, rating_id DESC
    ) AS lifetime_position,
    ROW_NUMBER() OVER (
        PARTITION BY rater_user_id, rated_user_id, season_year
        ORDER BY revealed_at DESC, rating_id DESC
    ) AS season_position
FROM (
    SELECT
        rating.id AS rating_id,
        obligation.engagement_request_id,
        obligation.rater_user_id,
        obligation.rated_user_id,
        rating.score,
        rating.revealed_at,
        EXTRACT(YEAR FROM rating.revealed_at AT TIME ZONE 'UTC')::INTEGER AS season_year
    FROM ratings rating
    JOIN rating_obligations obligation ON obligation.id = rating.obligation_id
    WHERE rating.revealed_at IS NOT NULL
) revealed_ratings;
