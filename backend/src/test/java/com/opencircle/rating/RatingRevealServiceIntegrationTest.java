package com.opencircle.rating;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.notification.NotificationBroadcaster;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class RatingRevealServiceIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private RatingRevealService ratingRevealService;
    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private NotificationBroadcaster broadcaster;

    private final List<Fixture> createdFixtures = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Fixture fixture : createdFixtures) {
            jdbc.update("DELETE FROM notifications WHERE resource_id = ?", fixture.engagementId());
            jdbc.update("DELETE FROM invite_posts WHERE id = ?", fixture.postId());
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", fixture.firstUserId(), fixture.secondUserId());
        }
        createdFixtures.clear();
        reset(broadcaster);
    }

    @Test
    void revealReturnsAndNotifiesEachRatingOnlyOnce() {
        Fixture fixture = fixture("repeat");

        int firstResult = reveal();
        int repeatedResult = reveal();

        assertThat(firstResult).isEqualTo(2);
        assertThat(repeatedResult).isZero();
        assertThat(revealedRatingCount(fixture)).isEqualTo(2);
        assertThat(notificationRows(fixture)).containsExactlyInAnyOrder(
                notification(fixture.firstUserId(), fixture.secondUserId()),
                notification(fixture.secondUserId(), fixture.firstUserId())
        );
        verify(broadcaster, timeout(1_000).times(1)).broadcast(
                eq(fixture.firstUserId()),
                any(RealtimeNotificationResponse.class)
        );
        verify(broadcaster, timeout(1_000).times(1)).broadcast(
                eq(fixture.secondUserId()),
                any(RealtimeNotificationResponse.class)
        );
    }

    @Test
    void concurrentRevealAttemptsCannotDuplicateNotifications() throws Exception {
        Fixture fixture = fixture("concurrent");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            List<Future<Integer>> results = List.of(
                    executor.submit(() -> revealWhenReleased(ready, start)),
                    executor.submit(() -> revealWhenReleased(ready, start))
            );
            ready.await();
            start.countDown();

            assertThat(results).extracting(this::getResult).containsExactlyInAnyOrder(0, 2);
            assertThat(revealedRatingCount(fixture)).isEqualTo(2);
            assertThat(notificationRows(fixture)).hasSize(2);
        } finally {
            executor.shutdownNow();
        }
    }

    private int reveal() {
        return transactions.execute(status -> ratingRevealService.revealResolvedRatings(NOW));
    }

    private int revealWhenReleased(CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent reveal interrupted", exception);
        }
        return reveal();
    }

    private int getResult(Future<Integer> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new IllegalStateException("Concurrent reveal failed", exception);
        }
    }

    private int revealedRatingCount(Fixture fixture) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM ratings rating
                JOIN rating_obligations obligation ON obligation.id = rating.obligation_id
                WHERE obligation.engagement_request_id = ?
                  AND rating.revealed_at = ?
                """,
                Integer.class,
                fixture.engagementId(),
                Timestamp.from(NOW)
        );
    }

    private List<Map<String, Object>> notificationRows(Fixture fixture) {
        return jdbc.queryForList(
                """
                SELECT recipient_user_id, actor_user_id
                FROM notifications
                WHERE type = 'RATING_REVEALED'
                  AND resource_type = 'ENGAGEMENT_REQUEST'
                  AND resource_id = ?
                  AND context_type = 'INVITE_POST'
                  AND context_id = ?
                """,
                fixture.engagementId(),
                fixture.postId()
        );
    }

    private Map<String, Object> notification(UUID recipientUserId, UUID actorUserId) {
        return Map.of(
                "recipient_user_id", recipientUserId,
                "actor_user_id", actorUserId
        );
    }

    private Fixture fixture(String key) {
        AppUser firstUser = user("first-" + key);
        AppUser secondUser = user("second-" + key);
        InvitePost post = posts.save(new InvitePost(
                firstUser,
                "Rating reveal " + key,
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(10_800)
        ));
        UUID engagementId = UUID.randomUUID();
        insertEngagement(engagementId, post, secondUser);
        insertSubmittedRating(engagementId, firstUser.getId(), secondUser.getId(), 5);
        insertSubmittedRating(engagementId, secondUser.getId(), firstUser.getId(), 4);

        Fixture fixture = new Fixture(
                engagementId,
                post.getId(),
                firstUser.getId(),
                secondUser.getId()
        );
        createdFixtures.add(fixture);
        return fixture;
    }

    private void insertEngagement(UUID engagementId, InvitePost post, AppUser requester) {
        Instant acceptedAt = NOW.minusSeconds(7_200);
        jdbc.update(
                """
                INSERT INTO engagement_requests (
                    id, invite_post_id, requester_id, status, expires_at,
                    responded_at, created_at, updated_at
                ) VALUES (?, ?, ?, 'ACCEPTED', ?, ?, ?, ?)
                """,
                engagementId,
                post.getId(),
                requester.getId(),
                Timestamp.from(post.getExpiresAt()),
                Timestamp.from(acceptedAt),
                Timestamp.from(acceptedAt.minusSeconds(300)),
                Timestamp.from(acceptedAt)
        );
    }

    private void insertSubmittedRating(
            UUID engagementId,
            UUID raterUserId,
            UUID ratedUserId,
            int score
    ) {
        UUID obligationId = UUID.randomUUID();
        Instant requiredAt = NOW.minusSeconds(7_200);
        Instant submittedAt = NOW.minusSeconds(3_600);
        jdbc.update(
                """
                INSERT INTO rating_obligations (
                    id, engagement_request_id, rater_user_id, rated_user_id,
                    status, requirement_trigger, required_at, due_at,
                    submitted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'SUBMITTED', 'CHAT_INACTIVITY', ?, ?, ?, ?, ?)
                """,
                obligationId,
                engagementId,
                raterUserId,
                ratedUserId,
                Timestamp.from(requiredAt),
                Timestamp.from(requiredAt.plusSeconds(86_400)),
                Timestamp.from(submittedAt),
                Timestamp.from(requiredAt.minusSeconds(300)),
                Timestamp.from(submittedAt)
        );
        jdbc.update(
                """
                INSERT INTO ratings (id, obligation_id, score, submitted_at)
                VALUES (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                obligationId,
                score,
                Timestamp.from(submittedAt)
        );
    }

    private AppUser user(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Rating",
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

    private record Fixture(
            UUID engagementId,
            UUID postId,
            UUID firstUserId,
            UUID secondUserId
    ) {
    }
}
