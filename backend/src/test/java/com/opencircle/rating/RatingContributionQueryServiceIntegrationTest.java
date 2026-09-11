package com.opencircle.rating;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.engagement.EngagementRequest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatingContributionQueryServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private RatingEnrollmentService enrollmentService;
    @Autowired private RatingObligationRepository obligations;
    @Autowired private RatingContributionQueryService contributionQueries;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    void latestRatingPerDistinctRaterControlsLifetimeAndSeasonContributions() {
        AppUser ratedUser = user("rated.contributions@example.com");
        AppUser repeatRater = user("repeat.rater@example.com");
        AppUser distinctRater = user("distinct.rater@example.com");

        addRevealedRating(
                repeatRater,
                ratedUser,
                1,
                Instant.parse("2026-01-10T12:00:00Z"),
                "first-repeat"
        );
        addRevealedRating(
                repeatRater,
                ratedUser,
                5,
                Instant.parse("2026-02-10T12:00:00Z"),
                "latest-repeat"
        );
        addRevealedRating(
                distinctRater,
                ratedUser,
                3,
                Instant.parse("2026-03-10T12:00:00Z"),
                "distinct"
        );

        List<RatingContribution> lifetime =
                contributionQueries.findLatestLifetimeContributions(ratedUser.getId());
        List<RatingContribution> season =
                contributionQueries.findLatestSeasonContributions(ratedUser.getId(), 2026);
        LifetimeReputationSummary summary = contributionQueries.getLifetimeSummary(ratedUser.getId());

        assertThat(lifetime)
                .extracting(RatingContribution::score)
                .containsExactlyInAnyOrder(5, 3);
        assertThat(season)
                .extracting(RatingContribution::score)
                .containsExactlyInAnyOrder(5, 3);
        assertThat(summary.averageRating()).isEqualByComparingTo(new BigDecimal("4.00"));
        assertThat(summary.totalRatingsReceived()).isEqualTo(3);
        assertThat(summary.distinctRaterCount()).isEqualTo(2);
    }

    @Test
    void equalRevealTimestampsUseRatingIdAsTheDeterministicTiebreaker() {
        AppUser ratedUser = user("rated.equal-timestamp@example.com");
        AppUser rater = user("rater.equal-timestamp@example.com");
        Instant revealedAt = Instant.parse("2026-04-10T12:00:00Z");
        UUID lowerRatingId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherRatingId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        addRevealedRating(rater, ratedUser, 1, revealedAt, "equal-lower", lowerRatingId);
        addRevealedRating(rater, ratedUser, 5, revealedAt, "equal-higher", higherRatingId);

        assertThat(contributionQueries.findLatestLifetimeContributions(ratedUser.getId()))
                .singleElement()
                .satisfies(contribution -> {
                    assertThat(contribution.ratingId()).isEqualTo(higherRatingId);
                    assertThat(contribution.score()).isEqualTo(5);
                });
        assertThat(contributionQueries.findLatestSeasonContributions(ratedUser.getId(), 2026))
                .singleElement()
                .satisfies(contribution -> assertThat(contribution.ratingId()).isEqualTo(higherRatingId));
    }

    private void addRevealedRating(
            AppUser rater,
            AppUser ratedUser,
            int score,
            Instant revealedAt,
            String key
    ) {
        addRevealedRating(rater, ratedUser, score, revealedAt, key, UUID.randomUUID());
    }

    private void addRevealedRating(
            AppUser rater,
            AppUser ratedUser,
            int score,
            Instant revealedAt,
            String key,
            UUID ratingId
    ) {
        Instant acceptedAt = revealedAt.minusSeconds(5 * 24 * 60 * 60L);
        InvitePost post = posts.save(new InvitePost(
                rater,
                "Contribution " + key,
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                acceptedAt.minusSeconds(3600)
        ));
        EngagementRequest engagement = new EngagementRequest(
                post,
                ratedUser,
                acceptedAt.minusSeconds(300)
        );
        entityManager.persist(engagement);
        engagement.accept(acceptedAt);
        post.recordAcceptedEngagement();
        enrollmentService.enrollAcceptedEngagement(engagement, acceptedAt);
        entityManager.flush();

        RatingObligation obligation = obligations.findPairForUpdate(engagement.getId()).stream()
                .filter(candidate -> candidate.getRater().getId().equals(rater.getId()))
                .findFirst()
                .orElseThrow();
        Instant requiredAt = revealedAt.minusSeconds(2 * 60 * 60);
        Instant submittedAt = revealedAt.minusSeconds(60 * 60);

        jdbc.update(
                """
                UPDATE rating_obligations
                SET status = 'SUBMITTED',
                    requirement_trigger = 'MAX_DURATION',
                    required_at = ?,
                    due_at = ?,
                    submitted_at = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                Timestamp.from(requiredAt),
                Timestamp.from(requiredAt.plusSeconds(24 * 60 * 60)),
                Timestamp.from(submittedAt),
                Timestamp.from(submittedAt),
                obligation.getId()
        );
        jdbc.update(
                """
                INSERT INTO ratings (id, obligation_id, score, submitted_at, revealed_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                ratingId,
                obligation.getId(),
                score,
                Timestamp.from(submittedAt),
                Timestamp.from(revealedAt)
        );
    }

    private AppUser user(String email) {
        return users.createUser(
                "Rating",
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
