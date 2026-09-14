package com.opencircle.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationCommandTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Test
    void rejectsMissingRecipient() {
        assertThatThrownBy(() -> command(null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification recipient is required");
    }

    @Test
    void rejectsIncompleteContext() {
        assertThatThrownBy(() -> command(
                UUID.randomUUID(),
                NotificationResourceType.INVITE_POST,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification context type and id must both be present or absent");
    }

    private NotificationCommand command(
            UUID recipientUserId,
            NotificationResourceType contextType,
            UUID contextId
    ) {
        return new NotificationCommand(
                recipientUserId,
                UUID.randomUUID(),
                NotificationType.ENGAGEMENT_REQUESTED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                UUID.randomUUID(),
                contextType,
                contextId,
                NOW
        );
    }
}
