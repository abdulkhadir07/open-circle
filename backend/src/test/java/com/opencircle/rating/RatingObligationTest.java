package com.opencircle.rating;

import com.opencircle.engagement.EngagementRequest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingObligationTest {

    private static final Instant ACCEPTED_AT = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void startsDormantAndBecomesRequiredForExactlyTwentyFourHours() {
        AppUser poster = user("poster.obligation@example.com");
        AppUser requester = user("requester.obligation@example.com");
        RatingObligation obligation = obligation(poster, requester);
        Instant requiredAt = ACCEPTED_AT.plusSeconds(300);

        assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.MONITORING);

        obligation.require(RatingTrigger.PARTICIPANT_EXIT, requiredAt);

        assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.REQUIRED);
        assertThat(obligation.getTrigger()).isEqualTo(RatingTrigger.PARTICIPANT_EXIT);
        assertThat(obligation.getRequiredAt()).isEqualTo(requiredAt);
        assertThat(obligation.getDueAt()).isEqualTo(requiredAt.plusSeconds(24 * 60 * 60));
    }

    @Test
    void exitsBeforeQualificationBecomePermanentlyNotRequired() {
        AppUser poster = user("poster.not-required@example.com");
        AppUser requester = user("requester.not-required@example.com");
        RatingObligation obligation = obligation(poster, requester);

        obligation.markNotRequired(ACCEPTED_AT.plusSeconds(60));
        obligation.require(RatingTrigger.MAX_DURATION, ACCEPTED_AT.plusSeconds(120));

        assertThat(obligation.getStatus()).isEqualTo(RatingObligationStatus.NOT_REQUIRED);
        assertThat(obligation.getTrigger()).isNull();
        assertThat(obligation.getDueAt()).isNull();
    }

    @Test
    void rejectsSelfRatingDirection() {
        AppUser user = user("self.obligation@example.com");
        EngagementRequest engagement = engagement(user("poster.self-direction@example.com"), user);

        assertThatThrownBy(() -> new RatingObligation(engagement, user, user, ACCEPTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Users cannot rate themselves");
    }

    @Test
    void ratingRejectsScoreOutsideOneToFive() {
        AppUser poster = user("poster.invalid-score@example.com");
        AppUser requester = user("requester.invalid-score@example.com");

        assertThatThrownBy(() -> new Rating(obligation(poster, requester), 0, ACCEPTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rating score must be between 1 and 5");
    }

    private RatingObligation obligation(AppUser poster, AppUser requester) {
        return new RatingObligation(engagement(poster, requester), poster, requester, ACCEPTED_AT);
    }

    private EngagementRequest engagement(AppUser poster, AppUser requester) {
        InvitePost post = new InvitePost(
                poster,
                "Rate this interaction",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                ACCEPTED_AT.minusSeconds(600)
        );
        EngagementRequest engagement = new EngagementRequest(post, requester, ACCEPTED_AT.minusSeconds(300));
        engagement.accept(ACCEPTED_AT);
        return engagement;
    }

    private AppUser user(String email) {
        return new AppUser(
                "rating_" + Math.abs(email.hashCode()),
                "Rating",
                "User",
                email,
                "hashed-password",
                "+1415" + Math.abs(email.hashCode()),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
