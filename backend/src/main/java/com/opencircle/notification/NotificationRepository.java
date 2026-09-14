package com.opencircle.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Modifying
    @Query(value = """
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
            ON CONFLICT ON CONSTRAINT uk_notifications_recipient_type_resource DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("recipientUserId") UUID recipientUserId,
            @Param("actorUserId") UUID actorUserId,
            @Param("type") String type,
            @Param("resourceType") String resourceType,
            @Param("resourceId") UUID resourceId,
            @Param("contextType") String contextType,
            @Param("contextId") UUID contextId,
            @Param("occurredAt") Instant occurredAt,
            @Param("createdAt") Instant createdAt
    );

    @Query(
            value = """
                    SELECT
                        notification.id AS "id",
                        notification.actor_user_id AS "actorUserId",
                        actor.username AS "actorUsername",
                        notification.type AS "type",
                        notification.resource_type AS "resourceType",
                        notification.resource_id AS "resourceId",
                        notification.context_type AS "contextType",
                        notification.context_id AS "contextId",
                        notification.occurrence_count AS "occurrenceCount",
                        notification.occurred_at AS "occurredAt",
                        notification.read_at AS "readAt"
                    FROM notifications notification
                    LEFT JOIN users actor ON actor.id = notification.actor_user_id
                    WHERE notification.recipient_user_id = :recipientUserId
                    ORDER BY notification.occurred_at DESC, notification.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM notifications notification
                    WHERE notification.recipient_user_id = :recipientUserId
                    """,
            nativeQuery = true
    )
    Page<NotificationRow> findInbox(
            @Param("recipientUserId") UUID recipientUserId,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                notification.id AS "id",
                notification.actor_user_id AS "actorUserId",
                actor.username AS "actorUsername",
                notification.type AS "type",
                notification.resource_type AS "resourceType",
                notification.resource_id AS "resourceId",
                notification.context_type AS "contextType",
                notification.context_id AS "contextId",
                notification.occurrence_count AS "occurrenceCount",
                notification.occurred_at AS "occurredAt",
                notification.read_at AS "readAt"
            FROM notifications notification
            LEFT JOIN users actor ON actor.id = notification.actor_user_id
            WHERE notification.id = :notificationId
              AND notification.recipient_user_id = :recipientUserId
            """, nativeQuery = true)
    Optional<NotificationRow> findRowByIdAndRecipient(
            @Param("notificationId") UUID notificationId,
            @Param("recipientUserId") UUID recipientUserId
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM notifications
            WHERE recipient_user_id = :recipientUserId
              AND read_at IS NULL
            """, nativeQuery = true)
    long countUnread(@Param("recipientUserId") UUID recipientUserId);

    @Modifying
    @Query(value = """
            UPDATE notifications
            SET read_at = COALESCE(read_at, :readAt)
            WHERE id = :notificationId
              AND recipient_user_id = :recipientUserId
            """, nativeQuery = true)
    int markRead(
            @Param("notificationId") UUID notificationId,
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query(value = """
            UPDATE notifications
            SET read_at = :readAt
            WHERE recipient_user_id = :recipientUserId
              AND read_at IS NULL
            """, nativeQuery = true)
    int markAllRead(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt
    );

    @Modifying
    @Query(value = """
            DELETE FROM notifications
            WHERE occurred_at < :cutoff
            """, nativeQuery = true)
    int deleteOccurredBefore(@Param("cutoff") Instant cutoff);
}
