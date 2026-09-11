package com.opencircle.rating;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.chat.ChatRoomService;
import com.opencircle.engagement.EngagementRequest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatingLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-11T12:00:00Z");

    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private ChatRoomService chatRooms;
    @Autowired private RatingEnrollmentService enrollmentService;
    @Autowired private RatingObligationRepository obligations;
    @Autowired private RatingRepository ratings;
    @Autowired private RatingService ratingService;
    @Autowired private RatingLifecycleService lifecycleService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void fixedTime() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void roomInactivityRequiresBothDirectionsAfterQualifiedPairMessages() {
        Interaction interaction = interaction("inactivity", NOW.minusSeconds(days(5)));
        addMessage(interaction, interaction.poster(), "Poster starts", NOW.minusSeconds(days(3) + 120));
        addMessage(
                interaction,
                interaction.requester(),
                "Requester shares an attachment",
                NOW.minusSeconds(days(3) + 60),
                "ATTACHMENT"
        );
        addMessage(interaction, interaction.poster(), "Poster follows up", NOW.minusSeconds(days(3)));

        RatingLifecycleResult firstRun = lifecycleService.runLifecycle();
        RatingLifecycleResult secondRun = lifecycleService.runLifecycle();

        List<RatingObligation> pair = obligations.findPairForUpdate(interaction.engagementId());
        assertThat(pair).hasSize(2).allSatisfy(obligation -> {
            assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.REQUIRED);
            assertThat(obligation.getTrigger()).isEqualTo(RatingTrigger.CHAT_INACTIVITY);
            assertThat(obligation.getRequiredAt()).isEqualTo(NOW);
            assertThat(obligation.getDueAt()).isEqualTo(NOW.plusSeconds(days(1)));
        });
        assertThat(firstRun.activatedEngagements()).isEqualTo(1);
        assertThat(secondRun.activatedEngagements()).isZero();
    }

    @Test
    void messagesFromOtherGroupRequestersDoNotQualifyThePair() {
        Instant acceptedAt = NOW.minusSeconds(days(15));
        AppUser poster = user("poster.group-qualification@example.com");
        InvitePost post = invitePost(poster, "group-qualification", acceptedAt);
        Interaction first = interaction(post, user("first.group-qualification@example.com"), acceptedAt);
        Interaction second = interaction(post, user("second.group-qualification@example.com"), acceptedAt);

        addMessage(first, poster, "Poster message one", NOW.minusSeconds(120));
        addMessage(first, second.requester(), "Other requester message", NOW.minusSeconds(60));
        addMessage(first, poster, "Poster message two", NOW.minusSeconds(30));

        lifecycleService.runLifecycle();

        assertThat(obligations.findPairForUpdate(first.engagementId()))
                .extracting(RatingObligation::getStatus)
                .containsOnly(RatingObligationStatus.MONITORING);
    }

    @Test
    void exitBeforeQualificationEndsPairWithoutRequirement() {
        Interaction interaction = interaction("short-exit", NOW.minusSeconds(600));
        addMessage(interaction, interaction.poster(), "Only one message", NOW.minusSeconds(300));

        lifecycleService.handleParticipantExit(
                interaction.postId(),
                interaction.requester().getId(),
                NOW
        );

        assertThat(obligations.findPairForUpdate(interaction.engagementId()))
                .extracting(RatingObligation::getStatus)
                .containsOnly(RatingObligationStatus.NOT_REQUIRED);
    }

    @Test
    void overdueObligationsArePenalizedOnceAtTheDeadline() {
        Interaction interaction = interaction("missed", NOW.minusSeconds(days(6)));
        addMessage(interaction, interaction.poster(), "Poster starts", NOW.minusSeconds(days(4) + 120));
        addMessage(interaction, interaction.requester(), "Requester replies", NOW.minusSeconds(days(4) + 60));
        addMessage(interaction, interaction.poster(), "Poster follows up", NOW.minusSeconds(days(4)));

        RatingLifecycleResult firstRun = lifecycleService.runLifecycle();
        RatingLifecycleResult retry = lifecycleService.runLifecycle();

        assertThat(obligations.findPairForUpdate(interaction.engagementId()))
                .hasSize(2)
                .allSatisfy(obligation -> {
                    assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.MISSED);
                    assertThat(obligation.getPenaltyPoints()).isEqualTo(-10);
                    assertThat(obligation.getPenalizedAt()).isEqualTo(NOW);
                });
        assertThat(firstRun.missedObligations()).isEqualTo(2);
        assertThat(retry.missedObligations()).isZero();
    }

    @Test
    void participantExitPreservesAnEarlierInactivityTrigger() {
        Interaction interaction = interaction("earliest-trigger", NOW.minusSeconds(days(5)));
        addMessage(interaction, interaction.poster(), "Poster starts", NOW.minusSeconds(days(4) + 120));
        addMessage(interaction, interaction.requester(), "Requester replies", NOW.minusSeconds(days(4) + 60));
        addMessage(interaction, interaction.poster(), "Poster follows up", NOW.minusSeconds(days(4)));

        lifecycleService.handleParticipantExit(
                interaction.postId(),
                interaction.requester().getId(),
                NOW
        );

        assertThat(obligations.findPairForUpdate(interaction.engagementId()))
                .allSatisfy(obligation -> {
                    assertThat(obligation.getTrigger()).isEqualTo(RatingTrigger.CHAT_INACTIVITY);
                    assertThat(obligation.getRequiredAt()).isEqualTo(NOW.minusSeconds(days(1)));
                });
    }

    @Test
    void maximumDurationTriggersAsSoonAsAQualifiedPairReachesFourteenDays() {
        Instant acceptedAt = NOW.minusSeconds(days(14));
        Interaction interaction = interaction("max-duration", acceptedAt);
        addMessage(interaction, interaction.poster(), "Poster starts", NOW.minusSeconds(120));
        addMessage(interaction, interaction.requester(), "Requester replies", NOW.minusSeconds(60));
        addMessage(interaction, interaction.poster(), "Third message", NOW);

        lifecycleService.runLifecycle();

        assertThat(obligations.findPairForUpdate(interaction.engagementId()))
                .allSatisfy(obligation -> {
                    assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.REQUIRED);
                    assertThat(obligation.getTrigger()).isEqualTo(RatingTrigger.MAX_DURATION);
                    assertThat(obligation.getRequiredAt()).isEqualTo(NOW);
                });
    }

    @Test
    void oneSubmittedRatingRevealsWhenTheOtherDirectionMissesItsDeadline() {
        Interaction interaction = interaction("single-submission", NOW.minusSeconds(days(5)));
        addMessage(interaction, interaction.poster(), "Poster starts", NOW.minusSeconds(days(3) + 120));
        addMessage(interaction, interaction.requester(), "Requester replies", NOW.minusSeconds(days(3) + 60));
        addMessage(interaction, interaction.poster(), "Poster follows up", NOW.minusSeconds(days(3)));
        lifecycleService.runLifecycle();

        Rating submitted = ratingService.submitRating(interaction.poster(), interaction.engagementId(), 5);
        assertThat(submitted.getRevealedAt()).isNull();

        when(clock.instant()).thenReturn(NOW.plusSeconds(days(1)));
        RatingLifecycleResult deadlineRun = lifecycleService.runLifecycle();
        Rating revealed = ratings.findDetailedById(submitted.getId()).orElseThrow();

        assertThat(deadlineRun.missedObligations()).isEqualTo(1);
        assertThat(deadlineRun.revealedRatings()).isEqualTo(1);
        assertThat(revealed.getRevealedAt()).isEqualTo(NOW.plusSeconds(days(1)));
        assertThat(obligations.findPairForUpdate(interaction.engagementId()))
                .extracting(RatingObligation::getStatus)
                .containsExactlyInAnyOrder(
                        RatingObligationStatus.SUBMITTED,
                        RatingObligationStatus.MISSED
                );
    }

    private Interaction interaction(String key, Instant acceptedAt) {
        AppUser poster = user("poster." + key + "@example.com");
        InvitePost post = invitePost(poster, key, acceptedAt);
        return interaction(post, user("requester." + key + "@example.com"), acceptedAt);
    }

    private Interaction interaction(InvitePost post, AppUser requester, Instant acceptedAt) {
        EngagementRequest engagement = new EngagementRequest(
                post,
                requester,
                acceptedAt.minusSeconds(300)
        );
        entityManager.persist(engagement);
        engagement.accept(acceptedAt);
        post.recordAcceptedEngagement();

        when(clock.instant()).thenReturn(acceptedAt);
        chatRooms.openRoomForAcceptedRequest(post, requester);
        when(clock.instant()).thenReturn(NOW);
        enrollmentService.enrollAcceptedEngagement(engagement, acceptedAt);
        entityManager.flush();

        UUID roomId = jdbc.queryForObject(
                "SELECT id FROM chat_rooms WHERE invite_post_id = ?",
                UUID.class,
                post.getId()
        );
        return new Interaction(engagement.getId(), post.getId(), roomId, post.getPoster(), requester);
    }

    private InvitePost invitePost(AppUser poster, String key, Instant acceptedAt) {
        return posts.save(new InvitePost(
                poster,
                "Rating lifecycle " + key,
                InviteType.GROUP,
                4,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                acceptedAt.minusSeconds(3600)
        ));
    }

    private void addMessage(Interaction interaction, AppUser sender, String body, Instant createdAt) {
        addMessage(interaction, sender, body, createdAt, "TEXT");
    }

    private void addMessage(
            Interaction interaction,
            AppUser sender,
            String body,
            Instant createdAt,
            String type
    ) {
        jdbc.update(
                """
                INSERT INTO chat_messages (id, chat_room_id, sender_id, body, created_at, type)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                interaction.roomId(),
                sender.getId(),
                body,
                Timestamp.from(createdAt),
                type
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

    private long days(long days) {
        return days * 24 * 60 * 60;
    }

    private String phoneNumber(String email) {
        long suffix = Integer.toUnsignedLong(email.hashCode()) % 10_000_000_000L;
        return "+1%010d".formatted(suffix);
    }

    private record Interaction(
            UUID engagementId,
            UUID postId,
            UUID roomId,
            AppUser poster,
            AppUser requester
    ) {
    }
}
