package com.opencircle.score;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
class CircleScoreQueryRepository {

    private static final String RANKED_SCORES_QUERY = """
            ranked_scores AS (
                SELECT
                    annual_score.user_id,
                    app_user.username,
                    annual_score.circle_score AS annual_score,
                    reputation.average_rating,
                    annual_score.distinct_rater_count,
                    RANK() OVER (
                        ORDER BY
                            annual_score.circle_score DESC,
                            reputation.average_rating DESC NULLS LAST,
                            annual_score.distinct_rater_count DESC
                    ) AS score_rank
                FROM annual_circle_scores annual_score
                JOIN users app_user ON app_user.id = annual_score.user_id
                LEFT JOIN lifetime_reputation reputation
                    ON reputation.user_id = annual_score.user_id
                WHERE annual_score.season_year = :seasonYear
                  AND annual_score.circle_score > 0
            )
            SELECT
                score_rank,
                user_id,
                username,
                annual_score,
                average_rating,
                distinct_rater_count
            FROM ranked_scores
            WHERE score_rank <= :maximumRank
            ORDER BY score_rank, user_id
            """;

    private static final String LIVE_RANKING_QUERY = """
            WITH lifetime_reputation AS (
                SELECT
                    rated_user_id AS user_id,
                    ROUND(AVG(score)::NUMERIC, 2) AS average_rating
                FROM rating_contributions
                WHERE lifetime_position = 1
                GROUP BY rated_user_id
            ),
            """ + RANKED_SCORES_QUERY;

    private static final String CLOSED_SEASON_RANKING_QUERY = """
            WITH cutoff_contributions AS (
                SELECT
                    rated_user_id,
                    rater_user_id,
                    score,
                    ROW_NUMBER() OVER (
                        PARTITION BY rater_user_id, rated_user_id
                        ORDER BY revealed_at DESC, rating_id DESC
                    ) AS cutoff_position
                FROM rating_contributions
                WHERE revealed_at < :reputationCutoff
            ),
            lifetime_reputation AS (
                SELECT
                    rated_user_id AS user_id,
                    ROUND(AVG(score)::NUMERIC, 2) AS average_rating
                FROM cutoff_contributions
                WHERE cutoff_position = 1
                GROUP BY rated_user_id
            ),
            """ + RANKED_SCORES_QUERY;

    private final NamedParameterJdbcTemplate jdbc;

    CircleScoreQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    CircleScoreSummary findSummary(UUID userId, int seasonYear) {
        return jdbc.queryForObject(
                """
                SELECT
                    COALESCE(
                        SUM(circle_score) FILTER (WHERE season_year = :seasonYear),
                        0
                    ) AS annual_score,
                    COALESCE(SUM(circle_score), 0) AS lifetime_score
                FROM annual_circle_scores
                WHERE user_id = :userId
                """,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("seasonYear", seasonYear),
                (resultSet, rowNumber) -> new CircleScoreSummary(
                        seasonYear,
                        resultSet.getLong("annual_score"),
                        resultSet.getLong("lifetime_score")
                )
        );
    }

    List<RankedScoreboardEntry> findTopRanks(int seasonYear, int maximumRank) {
        return findTopRanks(
                LIVE_RANKING_QUERY,
                new MapSqlParameterSource()
                        .addValue("seasonYear", seasonYear)
                        .addValue("maximumRank", maximumRank)
        );
    }

    List<RankedScoreboardEntry> findTopAwardCandidates(
            int seasonYear,
            Instant reputationCutoff
    ) {
        return findTopRanks(
                CLOSED_SEASON_RANKING_QUERY,
                new MapSqlParameterSource()
                        .addValue("seasonYear", seasonYear)
                        .addValue("maximumRank", 1)
                        .addValue("reputationCutoff", Timestamp.from(reputationCutoff))
        );
    }

    private List<RankedScoreboardEntry> findTopRanks(
            String query,
            MapSqlParameterSource parameters
    ) {
        return jdbc.query(
                query,
                parameters,
                this::mapRankedScore
        );
    }

    private RankedScoreboardEntry mapRankedScore(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new RankedScoreboardEntry(
                resultSet.getLong("score_rank"),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("username"),
                resultSet.getLong("annual_score"),
                resultSet.getBigDecimal("average_rating"),
                resultSet.getLong("distinct_rater_count")
        );
    }
}
