package com.opencircle.notification;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("test")
class AggregatedNotificationPublisherIntegrationTest extends AbstractIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private AggregatedNotificationPublisher publisher;
    @Autowired private NotificationRepository notifications;
    @Autowired private UserService users;
    @Autowired private TransactionTemplate transactions;

    @MockitoBean
    private NotificationBroadcaster broadcaster;

    @AfterEach
    void cleanup() {
        notifications.deleteAll();
        reset(broadcaster);
    }

    @Test
    void unreadPublicationsAggregateIntoOneNotificationWithLatestActivity() {
        AppUser recipient = user("aggregate-recipient");
        AppUser firstActor = user("aggregate-first-actor");
        AppUser latestActor = user("aggregate-latest-actor");
        UUID roomId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant firstOccurredAt = BASE_TIME.minusSeconds(60);
        Instant latestOccurredAt = firstOccurredAt.plusSeconds(30);

        publish(command(recipient, firstActor, roomId, postId, firstOccurredAt));
        UUID originalId = row(recipient).getId();
        publish(command(recipient, latestActor, roomId, postId, latestOccurredAt));

        NotificationRow row = row(recipient);
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(row.getId()).isEqualTo(originalId);
        assertThat(row.getActorUserId()).isEqualTo(latestActor.getId());
        assertThat(row.getOccurrenceCount()).isEqualTo(2);
        assertThat(row.getOccurredAt()).isEqualTo(latestOccurredAt);
        assertThat(row.getReadAt()).isNull();
    }

    @Test
    void olderPublicationIncrementsCountWithoutMovingLatestActivityBackward() {
        AppUser recipient = user("ordered-recipient");
        AppUser latestActor = user("ordered-latest-actor");
        AppUser olderActor = user("ordered-older-actor");
        UUID roomId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant latestOccurredAt = BASE_TIME.minusSeconds(10);

        publish(command(recipient, latestActor, roomId, postId, latestOccurredAt));
        publish(command(recipient, olderActor, roomId, postId, latestOccurredAt.minusSeconds(30)));

        NotificationRow row = row(recipient);
        assertThat(row.getActorUserId()).isEqualTo(latestActor.getId());
        assertThat(row.getOccurrenceCount()).isEqualTo(2);
        assertThat(row.getOccurredAt()).isEqualTo(latestOccurredAt);
    }

    @Test
    void activityAfterReadBecomesUnreadAndRestartsCountAtOne() {
        AppUser recipient = user("reset-recipient");
        AppUser actor = user("reset-actor");
        UUID roomId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Instant occurredAt = Instant.now().minusSeconds(30);

        publish(command(recipient, actor, roomId, postId, occurredAt));
        publish(command(recipient, actor, roomId, postId, occurredAt.plusSeconds(5)));
        UUID notificationId = row(recipient).getId();
        transactions.executeWithoutResult(status -> notifications.markRead(
                notificationId,
                recipient.getId(),
                Instant.now()
        ));

        publish(command(recipient, actor, roomId, postId, occurredAt.plusSeconds(10)));

        NotificationRow row = row(recipient);
        assertThat(row.getOccurrenceCount()).isEqualTo(1);
        assertThat(row.getReadAt()).isNull();
        assertThat(notifications.countUnread(recipient.getId())).isEqualTo(1);
    }

    @Test
    void concurrentPublicationsDoNotLoseIncrementsOrCreateDuplicates() throws Exception {
        AppUser recipient = user("concurrent-aggregate-recipient");
        AppUser actor = user("concurrent-aggregate-actor");
        NotificationCommand command = command(
                recipient,
                actor,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(5)
        );
        int publicationCount = 3;
        CountDownLatch ready = new CountDownLatch(publicationCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(publicationCount);

        try {
            List<Future<?>> results = new ArrayList<>();
            for (int index = 0; index < publicationCount; index++) {
                results.add(executor.submit(() -> publishWhenReleased(command, ready, start)));
            }

            ready.await();
            start.countDown();
            for (Future<?> result : results) {
                result.get();
            }

            assertThat(notifications.count()).isEqualTo(1);
            assertThat(row(recipient).getOccurrenceCount()).isEqualTo(publicationCount);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rollbackRemovesAggregatedWrite() {
        AppUser recipient = user("aggregate-rollback-recipient");
        AppUser actor = user("aggregate-rollback-actor");
        NotificationCommand command = command(
                recipient,
                actor,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(5)
        );

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            publisher.publishAggregated(command);
            throw new ExpectedRollbackException();
        })).isInstanceOf(ExpectedRollbackException.class);

        assertThat(notifications.count()).isZero();
    }

    @Test
    void publishingWithoutTransactionIsRejected() {
        AppUser recipient = user("aggregate-mandatory-recipient");
        AppUser actor = user("aggregate-mandatory-actor");

        assertThatThrownBy(() -> publisher.publishAggregated(command(
                recipient,
                actor,
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().minusSeconds(5)
        ))).isInstanceOf(IllegalTransactionStateException.class);
    }

    private void publish(NotificationCommand command) {
        transactions.executeWithoutResult(status -> publisher.publishAggregated(command));
    }

    private void publishWhenReleased(
            NotificationCommand command,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent publication interrupted", exception);
        }
        publish(command);
    }

    private NotificationRow row(AppUser recipient) {
        return notifications.findInbox(
                recipient.getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)
        ).getContent().getFirst();
    }

    private NotificationCommand command(
            AppUser recipient,
            AppUser actor,
            UUID roomId,
            UUID postId,
            Instant occurredAt
    ) {
        return new NotificationCommand(
                recipient.getId(),
                actor.getId(),
                NotificationType.CHAT_ACTIVITY,
                NotificationResourceType.CHAT_ROOM,
                roomId,
                NotificationResourceType.INVITE_POST,
                postId,
                occurredAt
        );
    }

    private AppUser user(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                label + "." + unique + "@example.com",
                "hashed-password",
                "+1415" + unique,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private static class ExpectedRollbackException extends RuntimeException {
    }
}
