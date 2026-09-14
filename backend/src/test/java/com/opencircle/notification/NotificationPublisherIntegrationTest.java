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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class NotificationPublisherIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NotificationPublisher publisher;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserService users;

    @Autowired
    private TransactionTemplate transactions;

    @MockitoBean
    private NotificationBroadcaster broadcaster;

    @AfterEach
    void cleanup() {
        notifications.deleteAll();
        reset(broadcaster);
    }

    @Test
    void persistsInCallerTransactionAndBroadcastsOnlyAfterCommit() {
        AppUser recipient = user("commit-recipient");
        AppUser actor = user("commit-actor");
        NotificationCommand command = command(recipient, actor, UUID.randomUUID());

        transactions.executeWithoutResult(status -> {
            publisher.publish(command);
            assertThat(notifications.count()).isEqualTo(1);
            org.mockito.Mockito.verifyNoInteractions(broadcaster);
        });

        verify(broadcaster, timeout(1_000)).broadcast(
                org.mockito.ArgumentMatchers.eq(recipient.getId()),
                any(RealtimeNotificationResponse.class)
        );
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void rollbackRemovesNotificationAndSuppressesBroadcast() {
        AppUser recipient = user("rollback-recipient");
        AppUser actor = user("rollback-actor");
        NotificationCommand command = command(recipient, actor, UUID.randomUUID());

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            publisher.publish(command);
            throw new ExpectedRollbackException();
        })).isInstanceOf(ExpectedRollbackException.class);

        assertThat(notifications.count()).isZero();
        org.mockito.Mockito.verifyNoInteractions(broadcaster);
    }

    @Test
    void concurrentDuplicatePublicationCreatesAndBroadcastsOnlyOnce() throws Exception {
        AppUser recipient = user("concurrent-recipient");
        AppUser actor = user("concurrent-actor");
        NotificationCommand command = command(recipient, actor, UUID.randomUUID());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<?>> results = List.of(
                    executor.submit(() -> publishWhenReleased(command, ready, start)),
                    executor.submit(() -> publishWhenReleased(command, ready, start))
            );

            ready.await();
            start.countDown();
            for (Future<?> result : results) {
                result.get();
            }

            assertThat(notifications.count()).isEqualTo(1);
            verify(broadcaster, timeout(1_000).times(1)).broadcast(
                    org.mockito.ArgumentMatchers.eq(recipient.getId()),
                    any(RealtimeNotificationResponse.class)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void realtimeFailureDoesNotUndoCommittedNotification() {
        AppUser recipient = user("failure-recipient");
        AppUser actor = user("failure-actor");
        NotificationCommand command = command(recipient, actor, UUID.randomUUID());
        doThrow(new IllegalStateException("Broker unavailable"))
                .when(broadcaster)
                .broadcast(any(), any());

        transactions.executeWithoutResult(status -> publisher.publish(command));

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void publishingWithoutAnActiveTransactionIsRejected() {
        AppUser recipient = user("mandatory-recipient");
        AppUser actor = user("mandatory-actor");

        assertThatThrownBy(() -> publisher.publish(command(recipient, actor, UUID.randomUUID())))
                .isInstanceOf(IllegalTransactionStateException.class);
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

        transactions.executeWithoutResult(status -> publisher.publish(command));
    }

    private NotificationCommand command(AppUser recipient, AppUser actor, UUID resourceId) {
        return new NotificationCommand(
                recipient.getId(),
                actor.getId(),
                NotificationType.ENGAGEMENT_REQUESTED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                resourceId,
                NotificationResourceType.INVITE_POST,
                UUID.randomUUID(),
                Instant.now()
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
