package com.opencircle.notification;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
class AggregatedNotificationRepository {

    private static final String UPSERT = """
            INSERT INTO notifications (
                id,
                recipient_user_id,
                actor_user_id,
                type,
                resource_type,
                resource_id,
                context_type,
                context_id,
                occurrence_count,
                occurred_at,
                created_at
            ) VALUES (
                :id,
                :recipientUserId,
                :actorUserId,
                :type,
                :resourceType,
                :resourceId,
                :contextType,
                :contextId,
                1,
                :occurredAt,
                :createdAt
            )
            ON CONFLICT ON CONSTRAINT uk_notifications_recipient_type_resource
            DO UPDATE SET
                actor_user_id = CASE
                    WHEN EXCLUDED.occurred_at >= notifications.occurred_at
                        THEN EXCLUDED.actor_user_id
                    ELSE notifications.actor_user_id
                END,
                context_type = EXCLUDED.context_type,
                context_id = EXCLUDED.context_id,
                occurrence_count = CASE
                    WHEN notifications.read_at IS NULL
                        THEN notifications.occurrence_count + 1
                    ELSE 1
                END,
                occurred_at = GREATEST(notifications.occurred_at, EXCLUDED.occurred_at),
                read_at = NULL,
                created_at = GREATEST(notifications.created_at, EXCLUDED.created_at)
            RETURNING id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    AggregatedNotificationRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    UUID upsert(UUID notificationId, NotificationCommand command, Instant createdAt) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", notificationId)
                .addValue("recipientUserId", command.recipientUserId())
                .addValue("actorUserId", command.actorUserId())
                .addValue("type", command.type().name())
                .addValue("resourceType", command.resourceType().name())
                .addValue("resourceId", command.resourceId())
                .addValue("contextType", command.contextType() == null ? null : command.contextType().name())
                .addValue("contextId", command.contextId())
                .addValue("occurredAt", Timestamp.from(command.occurredAt()))
                .addValue("createdAt", Timestamp.from(createdAt));

        return jdbc.queryForObject(UPSERT, parameters, UUID.class);
    }
}
