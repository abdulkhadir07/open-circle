package com.opencircle.notification;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final ProfileImageQueryService profileImages = mock(ProfileImageQueryService.class);
    private final NotificationQueryService service = new NotificationQueryService(notifications, profileImages);

    @Test
    void inboxMapsSemanticDataAndBatchesActorImages() {
        UUID recipientUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        NotificationRow row = row(
                notificationId,
                actorUserId,
                engagementId,
                postId,
                null
        );
        ProfileImageResponse profileImage = new ProfileImageResponse(
                UUID.randomUUID(),
                "https://example.com/actor.png",
                NOW.plusSeconds(3600),
                "image/png",
                NOW
        );

        when(notifications.findInbox(recipientUserId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));
        when(profileImages.getProfileImagesByUserIds(Set.of(actorUserId)))
                .thenReturn(Map.of(actorUserId, profileImage));

        NotificationInboxResponse response = service.getInbox(recipientUserId, 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.notifications()).singleElement().satisfies(notification -> {
            assertThat(notification.id()).isEqualTo(notificationId);
            assertThat(notification.type()).isEqualTo(NotificationType.ENGAGEMENT_REQUESTED);
            assertThat(notification.actor()).isEqualTo(new NotificationActorResponse(
                    actorUserId,
                    "actor_user",
                    profileImage
            ));
            assertThat(notification.resource()).isEqualTo(new NotificationResourceResponse(
                    NotificationResourceType.ENGAGEMENT_REQUEST,
                    engagementId
            ));
            assertThat(notification.context()).isEqualTo(new NotificationResourceResponse(
                    NotificationResourceType.INVITE_POST,
                    postId
            ));
            assertThat(notification.read()).isFalse();
        });
        verify(profileImages).getProfileImagesByUserIds(Set.of(actorUserId));
    }

    @Test
    void rejectsInvalidPaginationBeforeQuerying() {
        UUID recipientUserId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getInbox(recipientUserId, -1, 20))
                .isInstanceOf(InvalidNotificationPageException.class)
                .hasMessage("Page must be zero or greater");
        assertThatThrownBy(() -> service.getInbox(recipientUserId, 0, 101))
                .isInstanceOf(InvalidNotificationPageException.class)
                .hasMessage("Size must be between 1 and 100");
    }

    @Test
    void realtimeResponseIncludesCurrentUnreadCount() {
        UUID recipientUserId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        NotificationRow row = row(
                notificationId,
                actorUserId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                NOW.minusSeconds(30)
        );

        when(notifications.findRowByIdAndRecipient(notificationId, recipientUserId))
                .thenReturn(java.util.Optional.of(row));
        when(notifications.countUnread(recipientUserId)).thenReturn(4L);
        when(profileImages.getProfileImagesByUserIds(Set.of(actorUserId))).thenReturn(Map.of());

        RealtimeNotificationResponse response = service
                .getRealtimeResponse(notificationId, recipientUserId)
                .orElseThrow();

        assertThat(response.unreadCount()).isEqualTo(4);
        assertThat(response.notification().read()).isTrue();
    }

    private NotificationRow row(
            UUID notificationId,
            UUID actorUserId,
            UUID engagementId,
            UUID postId,
            Instant readAt
    ) {
        NotificationRow row = mock(NotificationRow.class);
        when(row.getId()).thenReturn(notificationId);
        when(row.getActorUserId()).thenReturn(actorUserId);
        when(row.getActorUsername()).thenReturn("actor_user");
        when(row.getType()).thenReturn(NotificationType.ENGAGEMENT_REQUESTED.name());
        when(row.getResourceType()).thenReturn(NotificationResourceType.ENGAGEMENT_REQUEST.name());
        when(row.getResourceId()).thenReturn(engagementId);
        when(row.getContextType()).thenReturn(NotificationResourceType.INVITE_POST.name());
        when(row.getContextId()).thenReturn(postId);
        when(row.getOccurrenceCount()).thenReturn(1);
        when(row.getOccurredAt()).thenReturn(NOW);
        when(row.getReadAt()).thenReturn(readAt);
        return row;
    }
}
