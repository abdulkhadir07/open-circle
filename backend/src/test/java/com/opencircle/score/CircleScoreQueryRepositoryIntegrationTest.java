package com.opencircle.score;

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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CircleScoreQueryRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicLong USER_SEQUENCE = new AtomicLong();

    @Autowired private CircleScoreQueryRepository scores;
    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    void usersWithoutScoreInputsReceiveZeroPrivateScoresAndNoPublicRank() {
        AppUser user = user("no-score-inputs");

        CircleScoreSummary summary = scores.findSummary(user.getId(), 2026);

        assertThat(summary.annualScore()).isZero();
        assertThat(summary.lifetimeScore()).isZero();
        assertThat(scores.findTopRanks(2026, 5)).isEmpty();
    }

    @Test
    void mapsEveryRatingValueToItsLockedCirclePoints() {
        Map<Integer, Long> expectedPoints = Map.of(
                1, 0L,
                2, 2L,
                3, 5L,
                4, 8L,
                5, 10L
        );

        expectedPoints.forEach((ratingScore, expectedCirclePoints) -> {
            AppUser ratedUser = user("points-rated-" + ratingScore);
            AppUser rater = user("points-rater-" + ratingScore);
            addRevealedRating(
                    rater,
                    ratedUser,
                    ratingScore,
                    Instant.parse("2026-04-10T12:00:00Z"),
                    "points-" + ratingScore
            );

            assertThat(scores.findSummary(ratedUser.getId(), 2026).annualScore())
                    .isEqualTo(expectedCirclePoints);
        });
    }

    @Test
    void annualScoresDeduplicateRatingsButAccumulateEveryPenaltyAcrossUtcYears() {
        AppUser scoredUser = user("annual-scored");
        AppUser repeatRater = user("annual-repeat-rater");
        AppUser otherRater = user("annual-other-rater");
        AppUser counterpart = user("annual-counterpart");

        addRevealedRating(
                repeatRater,
                scoredUser,
                4,
                Instant.parse("2025-12-31T23:59:59Z"),
                "previous-season-rating"
        );
        addMissedObligation(
                scoredUser,
                counterpart,
                Instant.parse("2025-12-31T23:59:59Z"),
                "previous-season-penalty"
        );

        addRevealedRating(
                repeatRater,
                scoredUser,
                5,
                Instant.parse("2026-01-10T12:00:00Z"),
                "superseded-rating"
        );
        addRevealedRating(
                repeatRater,
                scoredUser,
                2,
                Instant.parse("2026-02-10T12:00:00Z"),
                "latest-rating"
        );
        addRevealedRating(
                otherRater,
                scoredUser,
                4,
                Instant.parse("2026-03-10T12:00:00Z"),
                "distinct-rating"
        );

        addMissedObligation(
                scoredUser,
                counterpart,
                Instant.parse("2026-01-01T00:00:00Z"),
                "first-current-penalty"
        );
        addMissedObligation(
                scoredUser,
                counterpart,
                Instant.parse("2026-04-01T00:00:00Z"),
                "second-current-penalty"
        );
        addMissedObligation(
                scoredUser,
                counterpart,
                Instant.parse("2026-05-01T00:00:00Z"),
                "third-current-penalty"
        );
        addSubmittedObligation(
                scoredUser,
                counterpart,
                Instant.parse("2026-06-01T00:00:00Z"),
                "submitted-without-penalty"
        );

        CircleScoreSummary previousSeason = scores.findSummary(scoredUser.getId(), 2025);
        CircleScoreSummary currentSeason = scores.findSummary(scoredUser.getId(), 2026);
        Map<String, Object> currentComponents = annualComponents(scoredUser.getId(), 2026);

        assertThat(previousSeason.annualScore()).isEqualTo(-2);
        assertThat(previousSeason.lifetimeScore()).isEqualTo(-22);
        assertThat(currentSeason.annualScore()).isEqualTo(-20);
        assertThat(currentSeason.lifetimeScore()).isEqualTo(-22);
        assertThat(currentComponents)
                .containsEntry("rating_points", 10L)
                .containsEntry("penalty_points", -30L)
                .containsEntry("circle_score", -20L)
                .containsEntry("distinct_rater_count", 2L);
    }

    @Test
    void scoreboardUsesAllTieBreakersKeepsFifthRankTiesAndExcludesNonPositiveScores() {
        int seasonYear = 2026;
        Instant revealedAt = Instant.parse("2026-07-01T12:00:00Z");

        AppUser first = user("scoreboard-first");
        addRatingFromNewRater(first, 5, revealedAt, "first-a");
        addRatingFromNewRater(first, 5, revealedAt, "first-b");

        AppUser second = user("scoreboard-second");
        addRatingFromNewRater(second, 5, revealedAt, "second-a");
        addRatingFromNewRater(second, 5, revealedAt, "second-b");
        addMissedObligation(second, user("second-counterpart"), revealedAt, "second-penalty");

        AppUser third = user("scoreboard-third");
        addRatingFromNewRater(third, 5, revealedAt, "third");

        AppUser fourth = user("scoreboard-fourth");
        addRatingFromNewRater(fourth, 4, revealedAt, "fourth-a");
        addRatingFromNewRater(fourth, 2, revealedAt, "fourth-b");

        AppUser tiedFifthA = user("scoreboard-fifth-a");
        addRatingFromNewRater(tiedFifthA, 4, revealedAt, "fifth-a");

        AppUser tiedFifthB = user("scoreboard-fifth-b");
        addRatingFromNewRater(tiedFifthB, 4, revealedAt, "fifth-b");

        AppUser zeroScore = user("scoreboard-zero");
        addRatingFromNewRater(zeroScore, 5, revealedAt, "zero-rating");
        addMissedObligation(zeroScore, user("zero-counterpart"), revealedAt, "zero-penalty");

        AppUser negativeScore = user("scoreboard-negative");
        addMissedObligation(
                negativeScore,
                user("negative-counterpart"),
                revealedAt,
                "negative-penalty"
        );

        List<RankedScoreboardEntry> scoreboard = scores.findTopRanks(seasonYear, 5);

        assertThat(scoreboard).hasSize(6);
        assertThat(scoreboard.subList(0, 4))
                .extracting(RankedScoreboardEntry::userId)
                .containsExactly(first.getId(), second.getId(), third.getId(), fourth.getId());
        assertThat(scoreboard)
                .extracting(RankedScoreboardEntry::rank)
                .containsExactly(1L, 2L, 3L, 4L, 5L, 5L);
        assertThat(scoreboard.subList(4, 6))
                .extracting(RankedScoreboardEntry::userId)
                .containsExactlyInAnyOrder(tiedFifthA.getId(), tiedFifthB.getId());
        assertThat(scoreboard)
                .extracting(RankedScoreboardEntry::userId)
                .doesNotContain(zeroScore.getId(), negativeScore.getId());
        assertThat(scores.findSummary(first.getId(), seasonYear).annualScore())
                .isEqualTo(scoreboard.getFirst().annualScore());
    }

    @Test
    void awardCandidatesUseLifetimeReputationFrozenAtTheSeasonBoundary() {
        int seasonYear = 2025;
        Instant reputationCutoff = Instant.parse("2026-01-01T00:00:00Z");
        Instant previousSeason = Instant.parse("2025-06-01T12:00:00Z");

        AppUser seasonEndWinner = user("award-season-end-winner");
        AppUser firstWinnerRater = user("award-first-winner-rater");
        AppUser secondWinnerRater = user("award-second-winner-rater");
        addRevealedRating(
                firstWinnerRater,
                seasonEndWinner,
                5,
                previousSeason,
                "award-winner-first-rating"
        );
        addRevealedRating(
                secondWinnerRater,
                seasonEndWinner,
                5,
                previousSeason,
                "award-winner-second-rating"
        );
        addMissedObligation(
                seasonEndWinner,
                user("award-winner-counterpart"),
                previousSeason,
                "award-winner-penalty"
        );

        AppUser liveReputationWinner = user("award-live-reputation-winner");
        addRevealedRating(
                user("award-first-live-rater"),
                liveReputationWinner,
                3,
                previousSeason,
                "award-live-first-rating"
        );
        addRevealedRating(
                user("award-second-live-rater"),
                liveReputationWinner,
                3,
                previousSeason,
                "award-live-second-rating"
        );

        Instant followingSeason = Instant.parse("2026-03-01T12:00:00Z");
        addRevealedRating(
                firstWinnerRater,
                seasonEndWinner,
                1,
                followingSeason,
                "award-winner-later-first-rating"
        );
        addRevealedRating(
                secondWinnerRater,
                seasonEndWinner,
                1,
                followingSeason,
                "award-winner-later-second-rating"
        );

        assertThat(scores.findTopRanks(seasonYear, 1))
                .extracting(RankedScoreboardEntry::userId)
                .containsExactly(liveReputationWinner.getId());
        assertThat(scores.findTopAwardCandidates(seasonYear, reputationCutoff))
                .extracting(RankedScoreboardEntry::userId)
                .containsExactly(seasonEndWinner.getId());
    }

    private void addRatingFromNewRater(
            AppUser ratedUser,
            int score,
            Instant revealedAt,
            String key
    ) {
        addRevealedRating(user(key + "-rater"), ratedUser, score, revealedAt, key);
    }

    private void addRevealedRating(
            AppUser rater,
            AppUser ratedUser,
            int score,
            Instant revealedAt,
            String key
    ) {
        Instant submittedAt = revealedAt.minusSeconds(60 * 60);
        Instant requiredAt = submittedAt.minusSeconds(60 * 60);
        UUID obligationId = createObligation(
                rater,
                ratedUser,
                "SUBMITTED",
                requiredAt,
                submittedAt,
                null,
                key
        );

        jdbc.update(
                """
                INSERT INTO ratings (id, obligation_id, score, submitted_at, revealed_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                obligationId,
                score,
                Timestamp.from(submittedAt),
                Timestamp.from(revealedAt)
        );
    }

    private void addMissedObligation(
            AppUser penalizedUser,
            AppUser counterpart,
            Instant penalizedAt,
            String key
    ) {
        createObligation(
                penalizedUser,
                counterpart,
                "MISSED",
                penalizedAt.minusSeconds(24 * 60 * 60),
                null,
                penalizedAt,
                key
        );
    }

    private void addSubmittedObligation(
            AppUser rater,
            AppUser ratedUser,
            Instant submittedAt,
            String key
    ) {
        createObligation(
                rater,
                ratedUser,
                "SUBMITTED",
                submittedAt.minusSeconds(60 * 60),
                submittedAt,
                null,
                key
        );
    }

    private UUID createObligation(
            AppUser rater,
            AppUser ratedUser,
            String status,
            Instant requiredAt,
            Instant submittedAt,
            Instant penalizedAt,
            String key
    ) {
        Instant effectiveAt = penalizedAt == null ? submittedAt : penalizedAt;
        Instant acceptedAt = effectiveAt.minusSeconds(5 * 24 * 60 * 60L);
        UUID engagementId = acceptedEngagement(rater, ratedUser, acceptedAt, key);
        UUID obligationId = UUID.randomUUID();
        Instant dueAt = requiredAt.plusSeconds(24 * 60 * 60);

        jdbc.update(
                """
                INSERT INTO rating_obligations (
                    id,
                    engagement_request_id,
                    rater_user_id,
                    rated_user_id,
                    status,
                    requirement_trigger,
                    required_at,
                    due_at,
                    submitted_at,
                    penalty_points,
                    penalized_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'MAX_DURATION', ?, ?, ?, ?, ?, ?, ?)
                """,
                obligationId,
                engagementId,
                rater.getId(),
                ratedUser.getId(),
                status,
                Timestamp.from(requiredAt),
                Timestamp.from(dueAt),
                submittedAt == null ? null : Timestamp.from(submittedAt),
                penalizedAt == null ? null : -10,
                penalizedAt == null ? null : Timestamp.from(penalizedAt),
                Timestamp.from(acceptedAt),
                Timestamp.from(effectiveAt)
        );

        return obligationId;
    }

    private UUID acceptedEngagement(
            AppUser poster,
            AppUser requester,
            Instant acceptedAt,
            String key
    ) {
        InvitePost post = posts.save(new InvitePost(
                poster,
                "Circle Score " + key,
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                acceptedAt.minusSeconds(60 * 60)
        ));
        EngagementRequest engagement = new EngagementRequest(
                post,
                requester,
                acceptedAt.minusSeconds(5 * 60)
        );
        entityManager.persist(engagement);
        engagement.accept(acceptedAt);
        post.recordAcceptedEngagement();
        entityManager.flush();
        return engagement.getId();
    }

    private Map<String, Object> annualComponents(UUID userId, int seasonYear) {
        return jdbc.queryForMap(
                """
                SELECT rating_points, penalty_points, circle_score, distinct_rater_count
                FROM annual_circle_scores
                WHERE user_id = ?
                  AND season_year = ?
                """,
                userId,
                seasonYear
        );
    }

    private AppUser user(String key) {
        long sequence = USER_SEQUENCE.incrementAndGet();
        return users.createUser(
                "Score",
                "User",
                key + "." + sequence + "@example.com",
                "hashed-password",
                "+1999%010d".formatted(sequence),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
