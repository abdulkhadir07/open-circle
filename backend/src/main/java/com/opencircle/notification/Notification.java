package com.opencircle.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
class Notification {

    @Id
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false, updatable = false)
    private UUID recipientUserId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 40, updatable = false)
    private NotificationResourceType resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 40, updatable = false)
    private NotificationResourceType contextType;

    @Column(name = "context_id", updatable = false)
    private UUID contextId;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    UUID getId() {
        return id;
    }

    UUID getRecipientUserId() {
        return recipientUserId;
    }

    UUID getActorUserId() {
        return actorUserId;
    }

    NotificationType getType() {
        return type;
    }

    NotificationResourceType getResourceType() {
        return resourceType;
    }

    UUID getResourceId() {
        return resourceId;
    }

    NotificationResourceType getContextType() {
        return contextType;
    }

    UUID getContextId() {
        return contextId;
    }

    int getOccurrenceCount() {
        return occurrenceCount;
    }

    Instant getOccurredAt() {
        return occurredAt;
    }

    Instant getReadAt() {
        return readAt;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
