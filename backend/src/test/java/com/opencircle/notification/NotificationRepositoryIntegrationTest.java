package com.opencircle.notification;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserService users;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertIsIdempotentAndInboxIsDeterministicallyOrdered() {
        AppUser recipient = user("notification.recipient.order@example.com");
        AppUser actor = user("notification.actor.order@example.com");
        UUID sharedResourceId = UUID.randomUUID();

        UUID olderId = UUID.randomUUID();
        UUID newerId = UUID.randomUUID();
        assertThat(insert(
                olderId,
                recipient,
                actor,
                NotificationType.ENGAGEMENT_REQUESTED,
                sharedResourceId,
                NOW.minusSeconds(60),
                NOW
        )).isEqualTo(1);
        assertThat(insert(
                UUID.randomUUID(),
                recipient,
                actor,
                NotificationType.ENGAGEMENT_REQUESTED,
                sharedResourceId,
                NOW.minusSeconds(30),
                NOW
        )).isZero();
        assertThat(insert(
                newerId,
                recipient,
                actor,
                NotificationType.ENGAGEMENT_ACCEPTED,
                UUID.randomUUID(),
                NOW,
                NOW
        )).isEqualTo(1);

        Page<NotificationRow> inbox = notifications.findInbox(
                recipient.getId(),
                PageRequest.of(0, 20)
        );

        assertThat(inbox.getTotalElements()).isEqualTo(2);
        assertThat(inbox.getContent())
                .extracting(NotificationRow::getId)
                .containsExactly(newerId, olderId);
        assertThat(inbox.getContent().getLast().getActorUsername()).isEqualTo(actor.getUsername());
        assertThat(notifications.countUnread(recipient.getId())).isEqualTo(2);
    }

    @Test
    void markReadIsOwnershipSafeAndIdempotent() {
        AppUser recipient = user("notification.recipient.read@example.com");
        AppUser otherUser = user("notification.other.read@example.com");
        AppUser actor = user("notification.actor.read@example.com");
        UUID notificationId = UUID.randomUUID();
        insert(
                notificationId,
                recipient,
                actor,
                NotificationType.ENGAGEMENT_HELD,
                UUID.randomUUID(),
                NOW,
                NOW
        );

        Instant firstReadAt = NOW.plusSeconds(10);
        assertThat(notifications.markRead(notificationId, otherUser.getId(), firstReadAt)).isZero();
        assertThat(notifications.markRead(notificationId, recipient.getId(), firstReadAt)).isEqualTo(1);
        assertThat(notifications.markRead(
                notificationId,
                recipient.getId(),
                NOW.plusSeconds(20)
        )).isEqualTo(1);

        NotificationRow row = notifications
                .findRowByIdAndRecipient(notificationId, recipient.getId())
                .orElseThrow();
        assertThat(row.getReadAt()).isEqualTo(firstReadAt);
        assertThat(notifications.countUnread(recipient.getId())).isZero();
    }

    @Test
    void markAllReadOnlyChangesTheRecipientsUnreadRows() {
        AppUser recipient = user("notification.recipient.all@example.com");
        AppUser otherUser = user("notification.other.all@example.com");
        AppUser actor = user("notification.actor.all@example.com");
        insert(UUID.randomUUID(), recipient, actor, NotificationType.ENGAGEMENT_ACCEPTED,
                UUID.randomUUID(), NOW, NOW);
        insert(UUID.randomUUID(), recipient, actor, NotificationType.ENGAGEMENT_DECLINED,
                UUID.randomUUID(), NOW, NOW);
        insert(UUID.randomUUID(), otherUser, actor, NotificationType.ENGAGEMENT_REQUESTED,
                UUID.randomUUID(), NOW, NOW);

        assertThat(notifications.markAllRead(recipient.getId(), NOW.plusSeconds(10))).isEqualTo(2);

        assertThat(notifications.countUnread(recipient.getId())).isZero();
        assertThat(notifications.countUnread(otherUser.getId())).isEqualTo(1);
    }

    @Test
    void retentionDeletesReadAndUnreadRowsByOccurrenceTime() {
        AppUser recipient = user("notification.recipient.retention@example.com");
        AppUser actor = user("notification.actor.retention@example.com");
        UUID oldUnreadId = UUID.randomUUID();
        UUID oldReadId = UUID.randomUUID();
        UUID recentId = UUID.randomUUID();
        insert(oldUnreadId, recipient, actor, NotificationType.ENGAGEMENT_REQUESTED,
                UUID.randomUUID(), NOW.minus(91, ChronoUnit.DAYS), NOW);
        insert(oldReadId, recipient, actor, NotificationType.ENGAGEMENT_ACCEPTED,
                UUID.randomUUID(), NOW.minus(100, ChronoUnit.DAYS), NOW);
        notifications.markRead(oldReadId, recipient.getId(), NOW.plusSeconds(1));
        insert(recentId, recipient, actor, NotificationType.ENGAGEMENT_HELD,
                UUID.randomUUID(), NOW.minus(89, ChronoUnit.DAYS), NOW);

        assertThat(notifications.deleteOccurredBefore(NOW.minus(90, ChronoUnit.DAYS))).isEqualTo(2);

        assertThat(notifications.findInbox(recipient.getId(), PageRequest.of(0, 20)).getContent())
                .extracting(NotificationRow::getId)
                .containsExactly(recentId);
    }

    @Test
    void deletingAnActorKeepsTheNotificationAndClearsActorIdentity() {
        AppUser recipient = user("notification.recipient.deleted-actor@example.com");
        AppUser actor = user("notification.actor.deleted@example.com");
        UUID notificationId = UUID.randomUUID();
        insert(notificationId, recipient, actor, NotificationType.ENGAGEMENT_WITHDRAWN,
                UUID.randomUUID(), NOW, NOW);

        entityManager.flush();
        entityManager.clear();
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", actor.getId());

        NotificationRow row = notifications
                .findRowByIdAndRecipient(notificationId, recipient.getId())
                .orElseThrow();
        assertThat(row.getActorUserId()).isNull();
        assertThat(row.getActorUsername()).isNull();
    }

    private int insert(
            UUID notificationId,
            AppUser recipient,
            AppUser actor,
            NotificationType type,
            UUID resourceId,
            Instant occurredAt,
            Instant createdAt
    ) {
        return notifications.insertIfAbsent(
                notificationId,
                recipient.getId(),
                actor.getId(),
                type.name(),
                NotificationResourceType.ENGAGEMENT_REQUEST.name(),
                resourceId,
                NotificationResourceType.INVITE_POST.name(),
                UUID.randomUUID(),
                occurredAt,
                createdAt
        );
    }

    private AppUser user(String email) {
        return users.createUser(
                "Test",
                "User",
                email,
                "hashed-password",
                phoneNumber(email),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }
}
