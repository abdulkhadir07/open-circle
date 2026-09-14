package com.opencircle.notification;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationCleanupJobTest {

    @Test
    void cleanupDelegatesToNotificationService() {
        NotificationService service = mock(NotificationService.class);

        new NotificationCleanupJob(service).cleanup();

        verify(service).deleteExpired();
    }
}
