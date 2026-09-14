package com.opencircle.invitepost.expiration;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
class InvitePostExpirationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    InvitePostExpirationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<InvitePostExpirationCandidate> findDue(Instant now) {
        return jdbc.query(
                """
                SELECT id, poster_id, expires_at
                FROM invite_posts
                WHERE expires_at <= :now
                  AND expiration_notified_at IS NULL
                ORDER BY expires_at, id
                """,
                new MapSqlParameterSource("now", Timestamp.from(now)),
                (resultSet, rowNumber) -> new InvitePostExpirationCandidate(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("poster_id", UUID.class),
                        resultSet.getTimestamp("expires_at").toInstant()
                )
        );
    }

    int claim(UUID postId, Instant expiresAt, Instant processedAt) {
        return jdbc.update(
                """
                UPDATE invite_posts
                SET expiration_notified_at = :processedAt
                WHERE id = :postId
                  AND expires_at = :expiresAt
                  AND expires_at <= :processedAt
                  AND expiration_notified_at IS NULL
                """,
                new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("expiresAt", Timestamp.from(expiresAt))
                        .addValue("processedAt", Timestamp.from(processedAt))
        );
    }

    List<ExpiringEngagementRequest> findUnresolvedRequests(UUID postId) {
        return jdbc.query(
                """
                SELECT id, requester_id
                FROM engagement_requests
                WHERE invite_post_id = :postId
                  AND status IN ('PENDING', 'HELD')
                ORDER BY id
                """,
                new MapSqlParameterSource("postId", postId),
                (resultSet, rowNumber) -> new ExpiringEngagementRequest(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("requester_id", UUID.class)
                )
        );
    }
}
