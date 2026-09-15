package com.opencircle.rating;

import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationPublisher;
import com.opencircle.notification.NotificationResourceType;
import com.opencircle.notification.NotificationType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
class RatingNotificationService {

    private final NotificationPublisher notifications;

    RatingNotificationService(NotificationPublisher notifications) {
        this.notifications = notifications;
    }

    void notifyRequired(
            RatingLifecycleSnapshot snapshot,
            List<RatingObligation> obligations,
            Instant reconciledAt
    ) {
        obligations.stream()
                .filter(obligation -> obligation.getDueAt().isAfter(reconciledAt))
                .forEach(obligation -> notifications.publish(new NotificationCommand(
                        obligation.getRater().getId(),
                        obligation.getRatedUser().getId(),
                        NotificationType.RATING_REQUIRED,
                        NotificationResourceType.ENGAGEMENT_REQUEST,
                        snapshot.engagementId(),
                        NotificationResourceType.INVITE_POST,
                        snapshot.invitePostId(),
                        obligation.getRequiredAt()
                )));
    }

    void notifyRevealed(List<RevealedRating> revealedRatings) {
        revealedRatings.forEach(rating -> notifications.publish(new NotificationCommand(
                rating.ratedUserId(),
                rating.raterUserId(),
                NotificationType.RATING_REVEALED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                rating.engagementId(),
                NotificationResourceType.INVITE_POST,
                rating.invitePostId(),
                rating.revealedAt()
        )));
    }
}
