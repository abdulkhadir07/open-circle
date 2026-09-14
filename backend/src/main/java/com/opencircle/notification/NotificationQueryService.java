package com.opencircle.notification;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
class NotificationQueryService {

    static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notifications;
    private final ProfileImageQueryService profileImages;

    NotificationQueryService(
            NotificationRepository notifications,
            ProfileImageQueryService profileImages
    ) {
        this.notifications = notifications;
        this.profileImages = profileImages;
    }

    @Transactional(readOnly = true)
    NotificationInboxResponse getInbox(UUID recipientUserId, int page, int size) {
        validatePage(page, size);

        Page<NotificationRow> notificationPage = notifications.findInbox(
                recipientUserId,
                PageRequest.of(page, size)
        );

        return new NotificationInboxResponse(
                responsesFor(notificationPage.getContent()),
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    long getUnreadCount(UUID recipientUserId) {
        return notifications.countUnread(recipientUserId);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    Optional<RealtimeNotificationResponse> getRealtimeResponse(
            UUID notificationId,
            UUID recipientUserId
    ) {
        return notifications.findRowByIdAndRecipient(notificationId, recipientUserId)
                .map(row -> new RealtimeNotificationResponse(
                        responsesFor(List.of(row)).getFirst(),
                        notifications.countUnread(recipientUserId)
                ));
    }

    private List<NotificationResponse> responsesFor(List<NotificationRow> rows) {
        Set<UUID> actorUserIds = new LinkedHashSet<>();
        rows.stream()
                .map(NotificationRow::getActorUserId)
                .filter(java.util.Objects::nonNull)
                .forEach(actorUserIds::add);

        Map<UUID, ProfileImageResponse> profileImagesByUser =
                profileImages.getProfileImagesByUserIds(actorUserIds);

        return rows.stream()
                .map(row -> responseFor(row, profileImagesByUser))
                .toList();
    }

    private NotificationResponse responseFor(
            NotificationRow row,
            Map<UUID, ProfileImageResponse> profileImagesByUser
    ) {
        NotificationActorResponse actor = row.getActorUserId() == null
                ? null
                : new NotificationActorResponse(
                        row.getActorUserId(),
                        row.getActorUsername(),
                        profileImagesByUser.get(row.getActorUserId())
                );

        NotificationResourceResponse context = row.getContextType() == null
                ? null
                : new NotificationResourceResponse(
                        NotificationResourceType.valueOf(row.getContextType()),
                        row.getContextId()
                );

        return new NotificationResponse(
                row.getId(),
                NotificationType.valueOf(row.getType()),
                actor,
                new NotificationResourceResponse(
                        NotificationResourceType.valueOf(row.getResourceType()),
                        row.getResourceId()
                ),
                context,
                row.getOccurrenceCount(),
                row.getOccurredAt(),
                row.getReadAt() != null,
                row.getReadAt()
        );
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new InvalidNotificationPageException("Page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidNotificationPageException("Size must be between 1 and 100");
        }
    }
}
