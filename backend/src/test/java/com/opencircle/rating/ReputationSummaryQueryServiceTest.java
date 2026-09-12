package com.opencircle.rating;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReputationSummaryQueryServiceTest {

    private final RatingLifecycleService lifecycleService = mock(RatingLifecycleService.class);
    private final RatingContributionQueryService contributions = mock(RatingContributionQueryService.class);
    private final ReputationSummaryQueryService service = new ReputationSummaryQueryService(
            lifecycleService,
            contributions
    );

    @Test
    void reconcilesRatingsBeforeReadingTheSharedLifetimeSummary() {
        UUID userId = UUID.randomUUID();
        LifetimeReputationSummary expected = new LifetimeReputationSummary(
                new BigDecimal("4.50"),
                7,
                4
        );
        when(contributions.getLifetimeSummary(userId)).thenReturn(expected);

        LifetimeReputationSummary result = service.getLifetimeSummary(userId);

        assertThat(result).isEqualTo(expected);
        InOrder order = inOrder(lifecycleService, contributions);
        order.verify(lifecycleService).reconcileUser(userId);
        order.verify(contributions).getLifetimeSummary(userId);
    }
}
