package com.opencircle.invitepost.expiration;

import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationPublisher;
import com.opencircle.notification.NotificationResourceType;
import com.opencircle.notification.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
class InvitePostExpirationProcessor {

    private final InvitePostExpirationRepository expirations;
    private final NotificationPublisher notificationPublisher;

    InvitePostExpirationProcessor(
            InvitePostExpirationRepository expirations,
            NotificationPublisher notificationPublisher
    ) {
        this.expirations = expirations;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(InvitePostExpirationCandidate candidate, Instant processedAt) {
        if (expirations.claim(candidate.postId(), candidate.expiresAt(), processedAt) != 1) {
            return false;
        }

        notificationPublisher.publish(new NotificationCommand(
                candidate.posterUserId(),
                null,
                NotificationType.INVITE_POST_EXPIRED,
                NotificationResourceType.INVITE_POST,
                candidate.postId(),
                null,
                null,
                candidate.expiresAt()
        ));

        expirations.findUnresolvedRequests(candidate.postId())
                .forEach(request -> notificationPublisher.publish(new NotificationCommand(
                        request.requesterUserId(),
                        null,
                        NotificationType.ENGAGEMENT_REQUEST_EXPIRED,
                        NotificationResourceType.ENGAGEMENT_REQUEST,
                        request.requestId(),
                        NotificationResourceType.INVITE_POST,
                        candidate.postId(),
                        candidate.expiresAt()
                )));

        return true;
    }
}
