package com.opencircle.rating;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.notification.NotificationPublisher;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class RatingNotificationRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private RatingRevealService ratingRevealService;
    @Autowired private RatingLifecycleService ratingLifecycleService;
    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private NotificationPublisher notificationPublisher;

    private Fixture fixture;

    @AfterEach
    void cleanup() {
        if (fixture == null) {
            return;
        }
        jdbc.update("DELETE FROM chat_rooms WHERE invite_post_id = ?", fixture.postId());
        jdbc.update("DELETE FROM invite_posts WHERE id = ?", fixture.postId());
        jdbc.update("DELETE FROM users WHERE id IN (?, ?)", fixture.raterUserId(), fixture.ratedUserId());
    }

    @Test
    void notificationFailureRollsBackTheRevealTransition() {
        fixture = fixture();
        doThrow(new ExpectedNotificationFailure())
                .when(notificationPublisher)
                .publish(any());

        assertThatThrownBy(() -> transactions.executeWithoutResult(
                status -> ratingRevealService.revealResolvedRatings(NOW)
        )).isInstanceOf(ExpectedNotificationFailure.class);

        assertThat(jdbc.queryForObject(
                "SELECT revealed_at IS NULL FROM ratings WHERE id = ?",
                Boolean.class,
                fixture.ratingId()
        )).isTrue();
    }

    @Test
    void notificationFailureRollsBackTheRequiredTransition() {
        fixture = monitoringFixture();
        doThrow(new ExpectedNotificationFailure())
                .when(notificationPublisher)
                .publish(any());

        assertThatThrownBy(() -> transactions.executeWithoutResult(
                status -> ratingLifecycleService.reconcileEngagement(fixture.engagementId(), NOW)
        )).isInstanceOf(ExpectedNotificationFailure.class);

        assertThat(jdbc.queryForList(
                """
                SELECT status
                FROM rating_obligations
                WHERE engagement_request_id = ?
                ORDER BY id
                """,
                String.class,
                fixture.engagementId()
        )).containsExactly("MONITORING", "MONITORING");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_id = ?",
                Integer.class,
                fixture.engagementId()
        )).isZero();
    }

    private Fixture fixture() {
        AppUser rater = user("rollback-rater");
        AppUser ratedUser = user("rollback-rated");
        InvitePost post = posts.save(new InvitePost(
                rater,
                "Rating notification rollback",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(259_200)
        ));
        UUID engagementId = UUID.randomUUID();
        insertEngagement(engagementId, post, ratedUser);
        UUID submittedObligationId = insertSubmittedObligation(
                engagementId,
                rater.getId(),
                ratedUser.getId()
        );
        insertMissedObligation(engagementId, ratedUser.getId(), rater.getId());
        UUID ratingId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO ratings (id, obligation_id, score, submitted_at)
                VALUES (?, ?, 5, ?)
                """,
                ratingId,
                submittedObligationId,
                Timestamp.from(NOW.minusSeconds(169_200))
        );
        return new Fixture(engagementId, post.getId(), rater.getId(), ratedUser.getId(), ratingId);
    }

    private Fixture monitoringFixture() {
        AppUser poster = user("rollback-required-poster");
        AppUser requester = user("rollback-required-requester");
        InvitePost post = posts.save(new InvitePost(
                poster,
                "Required notification rollback",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(518_400)
        ));
        UUID engagementId = UUID.randomUUID();
        Instant acceptedAt = NOW.minusSeconds(432_000);
        insertEngagement(engagementId, post, requester, acceptedAt);
        insertMonitoringObligation(engagementId, poster.getId(), requester.getId(), acceptedAt);
        insertMonitoringObligation(engagementId, requester.getId(), poster.getId(), acceptedAt);

        UUID roomId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO chat_rooms (id, invite_post_id, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                """,
                roomId,
                post.getId(),
                Timestamp.from(acceptedAt),
                Timestamp.from(acceptedAt)
        );
        insertParticipant(roomId, poster.getId(), acceptedAt);
        insertParticipant(roomId, requester.getId(), acceptedAt);
        insertMessage(roomId, poster.getId(), "Poster starts", NOW.minusSeconds(259_320));
        insertMessage(roomId, requester.getId(), "Requester replies", NOW.minusSeconds(259_260));
        insertMessage(roomId, poster.getId(), "Poster follows up", NOW.minusSeconds(259_200));

        return new Fixture(engagementId, post.getId(), poster.getId(), requester.getId(), null);
    }

    private void insertEngagement(UUID engagementId, InvitePost post, AppUser requester) {
        Instant acceptedAt = NOW.minusSeconds(216_000);
        insertEngagement(engagementId, post, requester, acceptedAt);
    }

    private void insertEngagement(
            UUID engagementId,
            InvitePost post,
            AppUser requester,
            Instant acceptedAt
    ) {
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

    private void insertMonitoringObligation(
            UUID engagementId,
            UUID raterUserId,
            UUID ratedUserId,
            Instant createdAt
    ) {
        jdbc.update(
                """
                INSERT INTO rating_obligations (
                    id, engagement_request_id, rater_user_id, rated_user_id,
                    status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'MONITORING', ?, ?)
                """,
                UUID.randomUUID(),
                engagementId,
                raterUserId,
                ratedUserId,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt)
        );
    }

    private void insertParticipant(UUID roomId, UUID userId, Instant joinedAt) {
        jdbc.update(
                """
                INSERT INTO chat_room_participants (id, chat_room_id, user_id, joined_at)
                VALUES (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                roomId,
                userId,
                Timestamp.from(joinedAt)
        );
    }

    private void insertMessage(UUID roomId, UUID senderId, String body, Instant createdAt) {
        jdbc.update(
                """
                INSERT INTO chat_messages (id, chat_room_id, sender_id, body, created_at, type)
                VALUES (?, ?, ?, ?, ?, 'TEXT')
                """,
                UUID.randomUUID(),
                roomId,
                senderId,
                body,
                Timestamp.from(createdAt)
        );
    }

    private UUID insertSubmittedObligation(
            UUID engagementId,
            UUID raterUserId,
            UUID ratedUserId
    ) {
        UUID obligationId = UUID.randomUUID();
        Instant requiredAt = NOW.minusSeconds(172_800);
        Instant submittedAt = requiredAt.plusSeconds(3_600);
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
        return obligationId;
    }

    private void insertMissedObligation(
            UUID engagementId,
            UUID raterUserId,
            UUID ratedUserId
    ) {
        UUID obligationId = UUID.randomUUID();
        Instant requiredAt = NOW.minusSeconds(172_800);
        Instant dueAt = requiredAt.plusSeconds(86_400);
        jdbc.update(
                """
                INSERT INTO rating_obligations (
                    id, engagement_request_id, rater_user_id, rated_user_id,
                    status, requirement_trigger, required_at, due_at,
                    penalty_points, penalized_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'MISSED', 'CHAT_INACTIVITY', ?, ?, -10, ?, ?, ?)
                """,
                obligationId,
                engagementId,
                raterUserId,
                ratedUserId,
                Timestamp.from(requiredAt),
                Timestamp.from(dueAt),
                Timestamp.from(dueAt),
                Timestamp.from(requiredAt.minusSeconds(300)),
                Timestamp.from(dueAt)
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
            UUID raterUserId,
            UUID ratedUserId,
            UUID ratingId
    ) {
    }

    private static class ExpectedNotificationFailure extends RuntimeException {
    }
}
