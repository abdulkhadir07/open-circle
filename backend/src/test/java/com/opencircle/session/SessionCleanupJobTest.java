package com.opencircle.session;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionCleanupJobTest {

    @Test
    void runsDailyInUtcAndDelegatesCleanup() throws Exception {
        SessionService service = mock(SessionService.class);
        SessionCleanupJob job = new SessionCleanupJob(service);

        job.cleanup();

        verify(service).cleanupExpiredAndRevoked();
        Method method = SessionCleanupJob.class.getDeclaredMethod("cleanup");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("${app.session.cleanup-cron:0 15 3 * * *}");
        assertThat(scheduled.zone()).isEqualTo("UTC");
    }
}
