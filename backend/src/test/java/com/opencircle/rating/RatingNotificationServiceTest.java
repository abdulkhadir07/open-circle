package com.opencircle.rating;

import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationPublisher;
import com.opencircle.notification.NotificationResourceType;
import com.opencircle.notification.NotificationType;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RatingNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final NotificationPublisher publisher = mock(NotificationPublisher.class);
    private final RatingNotificationService service = new RatingNotificationService(publisher);

    @Test
    void requiredNotificationsAreDirectionalAndReferenceTheEngagementAndPost() {
        RatingLifecycleSnapshot snapshot = snapshot();
        UUID posterId = snapshot.posterUserId();
        UUID requesterId = snapshot.requesterUserId();
        RatingObligation posterToRequester = obligation(
                posterId,
                requesterId,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600)
        );
        RatingObligation requesterToPoster = obligation(
                requesterId,
                posterId,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600)
        );

        service.notifyRequired(snapshot, List.of(posterToRequester, requesterToPoster), NOW);

        ArgumentCaptor<NotificationCommand> commands = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(publisher, org.mockito.Mockito.times(2)).publish(commands.capture());
        assertThat(commands.getAllValues()).containsExactlyInAnyOrder(
                requiredCommand(snapshot, posterId, requesterId),
                requiredCommand(snapshot, requesterId, posterId)
        );
    }

    @Test
    void requiredNotificationIsSuppressedWhenItsSubmissionWindowAlreadyClosed() {
        RatingLifecycleSnapshot snapshot = snapshot();
        RatingObligation stale = obligation(
                snapshot.posterUserId(),
                snapshot.requesterUserId(),
                NOW.minusSeconds(86_400),
                NOW
        );

        service.notifyRequired(snapshot, List.of(stale), NOW);

        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revealedNotificationIdentifiesTheRaterWithoutIncludingScoreData() {
        RevealedRating rating = new RevealedRating(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NOW
        );

        service.notifyRevealed(List.of(rating));

        verify(publisher).publish(new NotificationCommand(
                rating.ratedUserId(),
                rating.raterUserId(),
                NotificationType.RATING_REVEALED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                rating.engagementId(),
                NotificationResourceType.INVITE_POST,
                rating.invitePostId(),
                NOW
        ));
    }

    private NotificationCommand requiredCommand(
            RatingLifecycleSnapshot snapshot,
            UUID recipientId,
            UUID actorId
    ) {
        return new NotificationCommand(
                recipientId,
                actorId,
                NotificationType.RATING_REQUIRED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                snapshot.engagementId(),
                NotificationResourceType.INVITE_POST,
                snapshot.invitePostId(),
                NOW.minusSeconds(60)
        );
    }

    private RatingObligation obligation(
            UUID raterUserId,
            UUID ratedUserId,
            Instant requiredAt,
            Instant dueAt
    ) {
        RatingObligation obligation = mock(RatingObligation.class);
        AppUser rater = mock(AppUser.class);
        AppUser ratedUser = mock(AppUser.class);
        when(rater.getId()).thenReturn(raterUserId);
        when(ratedUser.getId()).thenReturn(ratedUserId);
        when(obligation.getRater()).thenReturn(rater);
        when(obligation.getRatedUser()).thenReturn(ratedUser);
        when(obligation.getRequiredAt()).thenReturn(requiredAt);
        when(obligation.getDueAt()).thenReturn(dueAt);
        return obligation;
    }

    private RatingLifecycleSnapshot snapshot() {
        return new RatingLifecycleSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                NOW.minusSeconds(3600),
                NOW.minusSeconds(60),
                3,
                2,
                1
        );
    }
}
