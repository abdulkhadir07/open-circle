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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AnnualAwardServiceIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicLong USER_SEQUENCE = new AtomicLong();

    @Autowired private AnnualAwardService awardService;
    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private Clock clock;

    private final Set<Integer> fixtureSeasons = new HashSet<>();

    @AfterEach
    void cleanAwardFixtures() {
        fixtureSeasons.forEach(seasonYear -> {
            jdbc.update("DELETE FROM annual_awards WHERE season_year = ?", seasonYear);
            jdbc.update("DELETE FROM annual_award_finalizations WHERE season_year = ?", seasonYear);
        });
        jdbc.update("""
                DELETE FROM ratings
                WHERE obligation_id IN (
                    SELECT obligation.id
                    FROM rating_obligations obligation
                    JOIN users app_user
                      ON app_user.id = obligation.rater_user_id
                      OR app_user.id = obligation.rated_user_id
                    WHERE app_user.email LIKE 'annual-award-%'
                )
                """);
        jdbc.update("""
                DELETE FROM rating_obligations
                WHERE rater_user_id IN (
                    SELECT id FROM users WHERE email LIKE 'annual-award-%'
                )
                   OR rated_user_id IN (
                    SELECT id FROM users WHERE email LIKE 'annual-award-%'
                )
                """);
        jdbc.update("""
                DELETE FROM engagement_requests
                WHERE requester_id IN (
                    SELECT id FROM users WHERE email LIKE 'annual-award-%'
                )
                   OR invite_post_id IN (
                    SELECT post.id
                    FROM invite_posts post
                    JOIN users app_user ON app_user.id = post.poster_id
                    WHERE app_user.email LIKE 'annual-award-%'
                )
                """);
        jdbc.update("""
                DELETE FROM invite_posts
                WHERE poster_id IN (
                    SELECT id FROM users WHERE email LIKE 'annual-award-%'
                )
                """);
        jdbc.update("DELETE FROM users WHERE email LIKE 'annual-award-%'");
    }

    @Test
    void delayedRecoveryStoresExactCoWinnersOnceAndKeepsTheSnapshotImmutable() {
        int seasonYear = currentSeasonYear() - 1;
        fixtureSeasons.add(seasonYear);
        Instant revealedAt = middleOf(seasonYear);
        AppUser first = user("first-co-winner");
        AppUser second = user("second-co-winner");
        addRatingFromNewRater(first, 5, revealedAt, "first-co-winner-rating");
        addRatingFromNewRater(second, 5, revealedAt, "second-co-winner-rating");

        FinalizedAnnualAward initial = awardService.finalizePreviousSeason();

        AppUser lateDataLeader = user("late-data-leader");
        addRatingFromNewRater(lateDataLeader, 5, revealedAt, "late-leader-first-rating");
        addRatingFromNewRater(lateDataLeader, 5, revealedAt, "late-leader-second-rating");
        FinalizedAnnualAward repeated = awardService.finalizeSeason(seasonYear);

        assertThat(initial.seasonYear()).isEqualTo(seasonYear);
        assertThat(initial.winners())
                .extracting(AnnualAwardWinner::userId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(initial.winners())
                .extracting(AnnualAwardWinner::finalScore)
                .containsOnly(10L);
        assertThat(repeated).isEqualTo(initial);
        assertThat(repeated.winners())
                .extracting(AnnualAwardWinner::userId)
                .doesNotContain(lateDataLeader.getId());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM annual_awards WHERE season_year = ?",
                Long.class,
                seasonYear
        )).isEqualTo(2L);
    }

    @Test
    void finalizesASeasonWithoutCreatingAnAwardWhenNoPositiveCandidateExists() {
        int seasonYear = currentSeasonYear() - 20;
        fixtureSeasons.add(seasonYear);

        FinalizedAnnualAward award = awardService.finalizeSeason(seasonYear);
        FinalizedAnnualAward repeated = awardService.finalizeSeason(seasonYear);

        assertThat(award.winners()).isEmpty();
        assertThat(repeated).isEqualTo(award);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM annual_award_finalizations WHERE season_year = ?",
                Long.class,
                seasonYear
        )).isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM annual_awards WHERE season_year = ?",
                Long.class,
                seasonYear
        )).isZero();
    }

    @Test
    void concurrentFinalizersShareOneDatabaseClaimAndOneWinnerSnapshot() throws Exception {
        int seasonYear = currentSeasonYear() - 10;
        fixtureSeasons.add(seasonYear);
        AppUser winner = user("concurrent-winner");
        addRatingFromNewRater(
                winner,
                5,
                middleOf(seasonYear),
                "concurrent-winner-rating"
        );

        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<FinalizedAnnualAward> first = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return awardService.finalizeSeason(seasonYear);
            });
            Future<FinalizedAnnualAward> second = executor.submit(() -> {
                start.await(5, TimeUnit.SECONDS);
                return awardService.finalizeSeason(seasonYear);
            });

            assertThat(first.get(10, TimeUnit.SECONDS))
                    .isEqualTo(second.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM annual_awards WHERE season_year = ?",
                Long.class,
                seasonYear
        )).isEqualTo(1L);
    }

    @Test
    void rejectsFinalizationBeforeASeasonHasClosed() {
        assertThatThrownBy(() -> awardService.finalizeSeason(currentSeasonYear()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only completed seasons can be finalized");
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
        UUID engagementId = acceptedEngagement(
                rater,
                ratedUser,
                revealedAt.minusSeconds(5 * 24 * 60 * 60L),
                key
        );
        UUID obligationId = UUID.randomUUID();

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
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, 'SUBMITTED', 'MAX_DURATION', ?, ?, ?, ?, ?)
                """,
                obligationId,
                engagementId,
                rater.getId(),
                ratedUser.getId(),
                Timestamp.from(requiredAt),
                Timestamp.from(requiredAt.plusSeconds(24 * 60 * 60)),
                Timestamp.from(submittedAt),
                Timestamp.from(revealedAt.minusSeconds(5 * 24 * 60 * 60L)),
                Timestamp.from(submittedAt)
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

    private UUID acceptedEngagement(
            AppUser poster,
            AppUser requester,
            Instant acceptedAt,
            String key
    ) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            InvitePost post = posts.save(new InvitePost(
                    poster,
                    "Annual Award " + key,
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
        });
    }

    private AppUser user(String key) {
        long sequence = USER_SEQUENCE.incrementAndGet();
        return users.createUser(
                "Annual",
                "Winner",
                "annual-award-" + key + "." + sequence + "@example.com",
                "hashed-password",
                "+1888%07d".formatted(sequence),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private Instant middleOf(int seasonYear) {
        return LocalDate.of(seasonYear, 6, 1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    private int currentSeasonYear() {
        return Instant.now(clock).atZone(ZoneOffset.UTC).getYear();
    }
}
