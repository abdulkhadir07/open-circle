package com.opencircle.invitepost.expiration;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitePostExpirationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final InvitePostExpirationRepository expirations = mock(InvitePostExpirationRepository.class);
    private final InvitePostExpirationProcessor processor = mock(InvitePostExpirationProcessor.class);
    private final InvitePostExpirationService service = new InvitePostExpirationService(
            expirations,
            processor,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void processesEveryDuePostAndCountsSuccessfulClaims() {
        InvitePostExpirationCandidate first = candidate(NOW.minusSeconds(60));
        InvitePostExpirationCandidate second = candidate(NOW.minusSeconds(30));
        when(expirations.findDue(NOW)).thenReturn(List.of(first, second));
        when(processor.process(first, NOW)).thenReturn(true);
        when(processor.process(second, NOW)).thenReturn(false);

        assertThat(service.processDuePosts()).isEqualTo(1);

        verify(processor).process(first, NOW);
        verify(processor).process(second, NOW);
    }

    @Test
    void oneFailedPostDoesNotBlockLaterCandidates() {
        InvitePostExpirationCandidate failed = candidate(NOW.minusSeconds(60));
        InvitePostExpirationCandidate successful = candidate(NOW.minusSeconds(30));
        when(expirations.findDue(NOW)).thenReturn(List.of(failed, successful));
        when(processor.process(failed, NOW)).thenThrow(new IllegalStateException("write failed"));
        when(processor.process(successful, NOW)).thenReturn(true);

        assertThat(service.processDuePosts()).isEqualTo(1);

        verify(processor).process(successful, NOW);
    }

    private InvitePostExpirationCandidate candidate(Instant expiresAt) {
        return new InvitePostExpirationCandidate(UUID.randomUUID(), UUID.randomUUID(), expiresAt);
    }
}
