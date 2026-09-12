CREATE INDEX idx_rating_obligations_penalized_at
    ON rating_obligations (penalized_at, rater_user_id)
    WHERE status = 'MISSED';

CREATE VIEW annual_circle_scores AS
WITH score_components AS (
    SELECT
        rated_user_id AS user_id,
        season_year,
        CASE score
            WHEN 1 THEN 0
            WHEN 2 THEN 2
            WHEN 3 THEN 5
            WHEN 4 THEN 8
            WHEN 5 THEN 10
        END::BIGINT AS rating_points,
        0::BIGINT AS penalty_points,
        1::BIGINT AS distinct_rater_count
    FROM rating_contributions
    WHERE season_position = 1

    UNION ALL

    SELECT
        rater_user_id AS user_id,
        EXTRACT(YEAR FROM penalized_at AT TIME ZONE 'UTC')::INTEGER AS season_year,
        0::BIGINT AS rating_points,
        penalty_points::BIGINT,
        0::BIGINT AS distinct_rater_count
    FROM rating_obligations
    WHERE status = 'MISSED'
      AND penalty_points IS NOT NULL
      AND penalized_at IS NOT NULL
)
SELECT
    user_id,
    season_year,
    SUM(rating_points)::BIGINT AS rating_points,
    SUM(penalty_points)::BIGINT AS penalty_points,
    SUM(rating_points + penalty_points)::BIGINT AS circle_score,
    SUM(distinct_rater_count)::BIGINT AS distinct_rater_count
FROM score_components
GROUP BY user_id, season_year;
