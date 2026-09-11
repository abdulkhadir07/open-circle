package com.opencircle.rating;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RatingContributionQueryService {

    private static final String CONTRIBUTION_COLUMNS = """
            SELECT
                rating_id,
                engagement_request_id,
                rater_user_id,
                rated_user_id,
                score,
                revealed_at,
                season_year
            FROM rating_contributions
            """;

    private final NamedParameterJdbcTemplate jdbc;

    RatingContributionQueryService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<RatingContribution> findLatestLifetimeContributions(UUID ratedUserId) {
        return jdbc.query(
                CONTRIBUTION_COLUMNS + """
                         WHERE rated_user_id = :ratedUserId
                           AND lifetime_position = 1
                         ORDER BY revealed_at DESC, rating_id DESC
                        """,
                Map.of("ratedUserId", ratedUserId),
                this::mapContribution
        );
    }

    @Transactional(readOnly = true)
    public List<RatingContribution> findLatestSeasonContributions(UUID ratedUserId, int seasonYear) {
        return jdbc.query(
                CONTRIBUTION_COLUMNS + """
                         WHERE rated_user_id = :ratedUserId
                           AND season_year = :seasonYear
                           AND season_position = 1
                         ORDER BY revealed_at DESC, rating_id DESC
                        """,
                Map.of("ratedUserId", ratedUserId, "seasonYear", seasonYear),
                this::mapContribution
        );
    }

    @Transactional(readOnly = true)
    LifetimeReputationSummary getLifetimeSummary(UUID ratedUserId) {
        return jdbc.queryForObject(
                """
                SELECT
                    ROUND(
                        (AVG(score) FILTER (WHERE lifetime_position = 1))::NUMERIC,
                        2
                    ) AS average_rating,
                    COUNT(*) AS total_ratings_received,
                    COUNT(*) FILTER (WHERE lifetime_position = 1) AS distinct_rater_count
                FROM rating_contributions
                WHERE rated_user_id = :ratedUserId
                """,
                new MapSqlParameterSource("ratedUserId", ratedUserId),
                (resultSet, rowNumber) -> new LifetimeReputationSummary(
                        resultSet.getObject("average_rating", BigDecimal.class),
                        resultSet.getLong("total_ratings_received"),
                        resultSet.getLong("distinct_rater_count")
                )
        );
    }

    private RatingContribution mapContribution(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RatingContribution(
                resultSet.getObject("rating_id", UUID.class),
                resultSet.getObject("engagement_request_id", UUID.class),
                resultSet.getObject("rater_user_id", UUID.class),
                resultSet.getObject("rated_user_id", UUID.class),
                resultSet.getInt("score"),
                resultSet.getTimestamp("revealed_at").toInstant(),
                resultSet.getInt("season_year")
        );
    }
}
