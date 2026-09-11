package com.opencircle.rating;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
class RatingLifecycleQueryRepository {

    private static final String SNAPSHOTS = """
            WITH monitoring_engagements AS (
                SELECT DISTINCT engagement_request_id
                FROM rating_obligations
                WHERE status = 'MONITORING'
            ),
            snapshots AS (
                SELECT
                    engagement.id AS engagement_id,
                    post.id AS invite_post_id,
                    post.poster_id AS poster_user_id,
                    engagement.requester_id AS requester_user_id,
                    engagement.responded_at AS accepted_at,
                    GREATEST(
                        engagement.responded_at,
                        COALESCE(MAX(message.created_at), engagement.responded_at)
                    ) AS last_room_activity_at,
                    COUNT(message.id) FILTER (
                        WHERE message.created_at >= engagement.responded_at
                          AND message.sender_id IN (post.poster_id, engagement.requester_id)
                    ) AS pair_message_count,
                    COUNT(message.id) FILTER (
                        WHERE message.created_at >= engagement.responded_at
                          AND message.sender_id = post.poster_id
                    ) AS poster_message_count,
                    COUNT(message.id) FILTER (
                        WHERE message.created_at >= engagement.responded_at
                          AND message.sender_id = engagement.requester_id
                    ) AS requester_message_count
                FROM monitoring_engagements monitoring
                JOIN engagement_requests engagement ON engagement.id = monitoring.engagement_request_id
                JOIN invite_posts post ON post.id = engagement.invite_post_id
                JOIN chat_rooms room ON room.invite_post_id = post.id
                LEFT JOIN chat_messages message ON message.chat_room_id = room.id
                WHERE engagement.status = 'ACCEPTED'
                GROUP BY engagement.id, post.id
            )
            SELECT *
            FROM snapshots snapshot
            """;

    private final NamedParameterJdbcTemplate jdbc;

    RatingLifecycleQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<RatingLifecycleSnapshot> findReady(java.time.Instant now) {
        return query(
                """
                 WHERE snapshot.pair_message_count >= 3
                   AND snapshot.poster_message_count > 0
                   AND snapshot.requester_message_count > 0
                   AND (
                       snapshot.last_room_activity_at + INTERVAL '3 days' <= :now
                       OR snapshot.accepted_at + INTERVAL '14 days' <= :now
                   )
                 """,
                Map.of("now", Timestamp.from(now))
        );
    }

    List<RatingLifecycleSnapshot> findReadyForUser(UUID userId, java.time.Instant now) {
        return query(
                """
                 WHERE (snapshot.poster_user_id = :userId OR snapshot.requester_user_id = :userId)
                   AND snapshot.pair_message_count >= 3
                   AND snapshot.poster_message_count > 0
                   AND snapshot.requester_message_count > 0
                   AND (
                       snapshot.last_room_activity_at + INTERVAL '3 days' <= :now
                       OR snapshot.accepted_at + INTERVAL '14 days' <= :now
                   )
                 """,
                Map.of("userId", userId, "now", Timestamp.from(now))
        );
    }

    List<RatingLifecycleSnapshot> findMonitoringForEngagement(UUID engagementId) {
        return query(
                " WHERE snapshot.engagement_id = :engagementId",
                Map.of("engagementId", engagementId)
        );
    }

    List<RatingLifecycleSnapshot> findMonitoringForInvitePost(UUID invitePostId) {
        return query(
                " WHERE snapshot.invite_post_id = :invitePostId",
                Map.of("invitePostId", invitePostId)
        );
    }

    List<RatingLifecycleSnapshot> findMonitoringForExit(UUID invitePostId, UUID exitingUserId) {
        return query(
                """
                 WHERE snapshot.invite_post_id = :invitePostId
                   AND (
                       snapshot.poster_user_id = :exitingUserId
                       OR snapshot.requester_user_id = :exitingUserId
                   )
                 """,
                Map.of("invitePostId", invitePostId, "exitingUserId", exitingUserId)
        );
    }

    private List<RatingLifecycleSnapshot> query(String condition, Map<String, ?> parameters) {
        return jdbc.query(
                SNAPSHOTS + condition,
                new MapSqlParameterSource(parameters),
                this::mapSnapshot
        );
    }

    private RatingLifecycleSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RatingLifecycleSnapshot(
                resultSet.getObject("engagement_id", UUID.class),
                resultSet.getObject("invite_post_id", UUID.class),
                resultSet.getObject("poster_user_id", UUID.class),
                resultSet.getObject("requester_user_id", UUID.class),
                resultSet.getTimestamp("accepted_at").toInstant(),
                resultSet.getTimestamp("last_room_activity_at").toInstant(),
                resultSet.getLong("pair_message_count"),
                resultSet.getLong("poster_message_count"),
                resultSet.getLong("requester_message_count")
        );
    }
}
