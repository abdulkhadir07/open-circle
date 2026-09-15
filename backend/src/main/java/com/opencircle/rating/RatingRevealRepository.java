package com.opencircle.rating;

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
class RatingRevealRepository {

    private static final String REVEAL_RESOLVED = """
            UPDATE ratings rating
            SET revealed_at = :revealedAt
            FROM rating_obligations obligation
            JOIN engagement_requests engagement
              ON engagement.id = obligation.engagement_request_id
            WHERE obligation.id = rating.obligation_id
              AND rating.revealed_at IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM rating_obligations unresolved
                  WHERE unresolved.engagement_request_id = obligation.engagement_request_id
                    AND unresolved.status IN ('MONITORING', 'REQUIRED')
              )
            RETURNING
                rating.id AS rating_id,
                obligation.engagement_request_id AS engagement_id,
                engagement.invite_post_id,
                obligation.rater_user_id,
                obligation.rated_user_id,
                rating.revealed_at
            """;

    private final NamedParameterJdbcTemplate jdbc;

    RatingRevealRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<RevealedRating> revealResolved(Instant revealedAt) {
        return jdbc.query(
                REVEAL_RESOLVED,
                new MapSqlParameterSource("revealedAt", Timestamp.from(revealedAt)),
                this::mapRating
        );
    }

    private RevealedRating mapRating(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RevealedRating(
                resultSet.getObject("rating_id", UUID.class),
                resultSet.getObject("engagement_id", UUID.class),
                resultSet.getObject("invite_post_id", UUID.class),
                resultSet.getObject("rater_user_id", UUID.class),
                resultSet.getObject("rated_user_id", UUID.class),
                resultSet.getTimestamp("revealed_at").toInstant()
        );
    }
}
