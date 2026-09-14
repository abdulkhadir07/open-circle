package com.opencircle.invitepost.expiration;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.notification.NotificationBroadcaster;
import com.opencircle.notification.NotificationType;
import com.opencircle.notification.RealtimeNotificationResponse;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class InvitePostExpirationProcessorIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private InvitePostExpirationProcessor processor;
    @Autowired private InvitePostExpirationRepository expirations;
    @Autowired private InvitePostRepository invitePosts;
    @Autowired private UserService users;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean
    private NotificationBroadcaster broadcaster;

    @AfterEach
    void resetBroadcaster() {
        reset(broadcaster);
    }

    @Test
    void publishesSystemNotificationsForPosterAndOnlyUnresolvedRequesters() {
        InvitePost post = post("recipients");
        AppUser pendingRequester = user("pending");
        AppUser heldRequester = user("held");
        UUID pendingRequestId = request(post, pendingRequester, "PENDING");
        UUID heldRequestId = request(post, heldRequester, "HELD");
        request(post, user("accepted"), "ACCEPTED");
        request(post, user("declined"), "DECLINED");
        request(post, user("withdrawn"), "WITHDRAWN");
        InvitePostExpirationCandidate candidate = candidate(post);

        assertThat(processor.process(candidate, NOW)).isTrue();
        assertThat(processor.process(candidate, NOW.plusSeconds(30))).isFalse();

        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT recipient_user_id, actor_user_id, type, resource_type,
                       resource_id, context_type, context_id, occurred_at
                FROM notifications
                WHERE context_id = ? OR resource_id = ?
                ORDER BY type, recipient_user_id
                """,
                post.getId(),
                post.getId()
        );

        assertThat(rows).hasSize(3);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.get("actor_user_id")).isNull();
            assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(post.getExpiresAt());
        });
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("recipient_user_id")).isEqualTo(post.getPoster().getId());
            assertThat(row.get("type")).isEqualTo(NotificationType.INVITE_POST_EXPIRED.name());
            assertThat(row.get("resource_type")).isEqualTo("INVITE_POST");
            assertThat(row.get("resource_id")).isEqualTo(post.getId());
            assertThat(row.get("context_type")).isNull();
            assertThat(row.get("context_id")).isNull();
        });
        assertRequesterNotification(rows, pendingRequester.getId(), pendingRequestId, post.getId());
        assertRequesterNotification(rows, heldRequester.getId(), heldRequestId, post.getId());

        verify(broadcaster, timeout(1_000).times(3)).broadcast(
                any(UUID.class),
                any(RealtimeNotificationResponse.class)
        );
    }

    @Test
    void concurrentProcessorsClaimAndPublishForAPostOnlyOnce() throws Exception {
        InvitePost post = post("concurrent");
        InvitePostExpirationCandidate candidate = candidate(post);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(() -> processWhenReleased(candidate, ready, start)),
                    executor.submit(() -> processWhenReleased(candidate, ready, start))
            );

            ready.await();
            start.countDown();

            assertThat(results)
                    .extracting(this::resultOf)
                    .containsExactlyInAnyOrder(true, false);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notifications WHERE resource_id = ?",
                    Integer.class,
                    post.getId()
            )).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT expiration_notified_at FROM invite_posts WHERE id = ?",
                    Instant.class,
                    post.getId()
            )).isEqualTo(NOW);
            verify(broadcaster, timeout(1_000).times(1)).broadcast(
                    any(UUID.class),
                    any(RealtimeNotificationResponse.class)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean processWhenReleased(
            InvitePostExpirationCandidate candidate,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return processor.process(candidate, NOW);
    }

    private boolean resultOf(Future<Boolean> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new AssertionError("Concurrent expiration processing failed", exception);
        }
    }

    private InvitePostExpirationCandidate candidate(InvitePost post) {
        return expirations.findDue(NOW).stream()
                .filter(candidate -> candidate.postId().equals(post.getId()))
                .findFirst()
                .orElseThrow();
    }

    private InvitePost post(String label) {
        return invitePosts.saveAndFlush(new InvitePost(
                user(label + "-poster"),
                "Expiration processor test",
                InviteType.GROUP,
                5,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(25 * 60 * 60L)
        ));
    }

    private UUID request(InvitePost post, AppUser requester, String status) {
        UUID requestId = UUID.randomUUID();
        Instant createdAt = post.getExpiresAt().minusSeconds(60 * 60);
        Instant respondedAt = List.of("ACCEPTED", "DECLINED", "HELD").contains(status)
                ? createdAt.plusSeconds(60)
                : null;
        Instant withdrawnAt = "WITHDRAWN".equals(status) ? createdAt.plusSeconds(60) : null;

        jdbc.update(
                """
                INSERT INTO engagement_requests (
                    id, invite_post_id, requester_id, status, expires_at,
                    responded_at, withdrawn_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                requestId,
                post.getId(),
                requester.getId(),
                status,
                Timestamp.from(post.getExpiresAt()),
                timestamp(respondedAt),
                timestamp(withdrawnAt),
                Timestamp.from(createdAt),
                Timestamp.from(createdAt)
        );
        return requestId;
    }

    private AppUser user(String label) {
        String unique = label + "." + Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                unique + "@example.com",
                "hashed-password",
                "+1415" + Long.toUnsignedString(System.nanoTime()),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private void assertRequesterNotification(
            List<Map<String, Object>> rows,
            UUID requesterId,
            UUID requestId,
            UUID postId
    ) {
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.get("recipient_user_id")).isEqualTo(requesterId);
            assertThat(row.get("type")).isEqualTo(NotificationType.ENGAGEMENT_REQUEST_EXPIRED.name());
            assertThat(row.get("resource_type")).isEqualTo("ENGAGEMENT_REQUEST");
            assertThat(row.get("resource_id")).isEqualTo(requestId);
            assertThat(row.get("context_type")).isEqualTo("INVITE_POST");
            assertThat(row.get("context_id")).isEqualTo(postId);
        });
    }

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
