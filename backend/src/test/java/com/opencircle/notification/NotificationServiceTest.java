package com.opencircle.notification;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationProperties properties = new NotificationProperties();
    private final NotificationService service = new NotificationService(
            notifications,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void markReadUpdatesOwnedNotification() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(notifications.markRead(notificationId, userId, NOW)).thenReturn(1);

        service.markRead(userId, notificationId);

        verify(notifications).markRead(notificationId, userId, NOW);
    }

    @Test
    void markReadHidesMissingAndForeignNotificationsBehindNotFound() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        when(notifications.markRead(notificationId, userId, NOW)).thenReturn(0);

        assertThatThrownBy(() -> service.markRead(userId, notificationId))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void deleteExpiredUsesConfiguredRetentionWindow() {
        properties.setRetentionDays(90);
        Instant cutoff = NOW.minusSeconds(90L * 24 * 60 * 60);
        when(notifications.deleteOccurredBefore(cutoff)).thenReturn(7);

        assertThat(service.deleteExpired()).isEqualTo(7);
        verify(notifications).deleteOccurredBefore(cutoff);
    }
}
