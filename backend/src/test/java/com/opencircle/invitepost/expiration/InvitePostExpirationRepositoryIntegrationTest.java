package com.opencircle.invitepost.expiration;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitePostExpirationRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private InvitePostExpirationRepository expirations;
    @Autowired private InvitePostRepository invitePosts;
    @Autowired private UserService users;
    @Autowired private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findsOnlyUnprocessedPostsWhoseExpirationHasArrived() {
        InvitePost due = post("due", NOW.minusSeconds(25 * 60 * 60L));
        InvitePost future = post("future", NOW.minusSeconds(23 * 60 * 60L));
        InvitePost claimed = post("claimed", NOW.minusSeconds(26 * 60 * 60L));
        jdbc.update(
                "UPDATE invite_posts SET expiration_notified_at = expires_at WHERE id = ?",
                claimed.getId()
        );

        assertThat(expirations.findDue(NOW))
                .extracting(InvitePostExpirationCandidate::postId)
                .contains(due.getId())
                .doesNotContain(future.getId(), claimed.getId());
    }

    @Test
    void conditionalClaimCanSucceedOnlyOnce() {
        InvitePost post = post("claim-once", NOW.minusSeconds(25 * 60 * 60L));
        Instant processedAt = NOW.plusSeconds(30);

        assertThat(expirations.claim(post.getId(), post.getExpiresAt(), processedAt)).isEqualTo(1);
        assertThat(expirations.claim(post.getId(), post.getExpiresAt(), processedAt)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT expiration_notified_at FROM invite_posts WHERE id = ?",
                Instant.class,
                post.getId()
        )).isEqualTo(processedAt);
    }

    @Test
    void returnsOnlyPendingAndHeldEngagementRequestsInStableOrder() {
        InvitePost post = post("request-status", NOW.minusSeconds(25 * 60 * 60L));
        UUID pendingId = request(post, user("pending"), "PENDING");
        UUID heldId = request(post, user("held"), "HELD");
        request(post, user("accepted"), "ACCEPTED");
        request(post, user("declined"), "DECLINED");
        request(post, user("withdrawn"), "WITHDRAWN");

        List<ExpiringEngagementRequest> unresolved = expirations.findUnresolvedRequests(post.getId());

        assertThat(unresolved)
                .extracting(ExpiringEngagementRequest::requestId)
                .containsExactlyInAnyOrder(pendingId, heldId);
        assertThat(unresolved)
                .extracting(ExpiringEngagementRequest::requestId)
                .isSortedAccordingTo(Comparator.comparing(UUID::toString));
    }

    private InvitePost post(String label, Instant createdAt) {
        return invitePosts.saveAndFlush(new InvitePost(
                user(label + "-poster"),
                "Expiration repository test",
                InviteType.GROUP,
                5,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                createdAt
        ));
    }

    private UUID request(InvitePost post, AppUser requester, String status) {
        entityManager.flush();
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

    private Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
