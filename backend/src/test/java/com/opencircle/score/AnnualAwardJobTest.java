package com.opencircle.score;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnnualAwardJobTest {

    @Mock private AnnualAwardService awardService;

    @Test
    void runsDailyAfterTheUtcSeasonBoundaryAndDelegatesRecovery() throws Exception {
        AnnualAwardJob job = new AnnualAwardJob(awardService);

        job.finalizePreviousSeason();

        verify(awardService).finalizePreviousSeason();

        Method jobMethod = AnnualAwardJob.class.getDeclaredMethod("finalizePreviousSeason");
        Scheduled schedule = jobMethod.getAnnotation(Scheduled.class);
        assertThat(schedule.cron())
                .isEqualTo("${app.awards.finalization-job-cron:0 5 0 * * *}");
        assertThat(schedule.zone()).isEqualTo("UTC");
    }
}
