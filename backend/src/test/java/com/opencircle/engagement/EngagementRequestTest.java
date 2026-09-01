package com.opencircle.engagement;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EngagementRequestTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-30T12:00:00Z");

    @Test
    void constructorCreatesPendingRequestThatExpiresWithInvitePost() {
        InvitePost post = invitePost();
        AppUser requester = user("requester@example.com");

        EngagementRequest request = new EngagementRequest(post, requester, CREATED_AT);

        assertThat(request.getInvitePost()).isEqualTo(post);
        assertThat(request.getRequester()).isEqualTo(requester);
        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.PENDING);
        assertThat(request.getExpiresAt()).isEqualTo(post.getExpiresAt());
        assertThat(request.getRespondedAt()).isNull();
        assertThat(request.getWithdrawnAt()).isNull();
        assertThat(request.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(request.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void acceptMarksRequestAccepted() {
        EngagementRequest request = engagementRequest();
        Instant respondedAt = CREATED_AT.plusSeconds(60);

        request.accept(respondedAt);

        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.ACCEPTED);
        assertThat(request.getRespondedAt()).isEqualTo(respondedAt);
    }

    @Test
    void declineMarksRequestDeclined() {
        EngagementRequest request = engagementRequest();
        Instant respondedAt = CREATED_AT.plusSeconds(60);

        request.decline(respondedAt);

        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.DECLINED);
        assertThat(request.getRespondedAt()).isEqualTo(respondedAt);
    }

    @Test
    void holdMarksRequestHeld() {
        EngagementRequest request = engagementRequest();
        Instant respondedAt = CREATED_AT.plusSeconds(60);

        request.hold(respondedAt);

        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.HELD);
        assertThat(request.getRespondedAt()).isEqualTo(respondedAt);
    }

    @Test
    void withdrawMarksPendingRequestWithdrawn() {
        EngagementRequest request = engagementRequest();
        Instant withdrawnAt = CREATED_AT.plusSeconds(60);

        request.withdraw(withdrawnAt);

        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.WITHDRAWN);
        assertThat(request.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    void withdrawMarksHeldRequestWithdrawn() {
        EngagementRequest request = engagementRequest();
        Instant respondedAt = CREATED_AT.plusSeconds(60);
        Instant withdrawnAt = CREATED_AT.plusSeconds(120);

        request.hold(respondedAt);
        request.withdraw(withdrawnAt);

        assertThat(request.getStatus()).isEqualTo(EngagementRequestStatus.WITHDRAWN);
        assertThat(request.getWithdrawnAt()).isEqualTo(withdrawnAt);
    }

    @Test
    void acceptedRequestCannotBeWithdrawn() {
        EngagementRequest request = engagementRequest();
        request.accept(CREATED_AT.plusSeconds(60));

        assertThatThrownBy(() -> request.withdraw(CREATED_AT.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only pending or held engagement requests can be withdrawn");
    }

    @Test
    void expiredRequestIsNotActionable() {
        EngagementRequest request = engagementRequest();
        Instant afterExpiration = request.getExpiresAt();

        assertThat(request.isExpired(afterExpiration)).isTrue();
        assertThat(request.isActionable(afterExpiration)).isFalse();

        assertThatThrownBy(() -> request.accept(afterExpiration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Engagement request is not actionable");
    }

    @Test
    void expiredRequestCannotBeWithdrawn() {
        EngagementRequest request = engagementRequest();
        Instant afterExpiration = request.getExpiresAt();

        assertThatThrownBy(() -> request.withdraw(afterExpiration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expired engagement requests cannot be withdrawn");
    }

    private EngagementRequest engagementRequest() {
        return new EngagementRequest(invitePost(), user("requester@example.com"), CREATED_AT);
    }

    private InvitePost invitePost() {
        return invitePost(user("poster@example.com"));
    }

    private InvitePost invitePost(AppUser poster) {
        return new InvitePost(
                poster,
                "Anyone want to hang out?",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                CREATED_AT
        );
    }

    private AppUser user(String email) {
        return new AppUser(
                email.substring(0, email.indexOf("@")).replace(".", "_") + "_1234",
                "Test",
                "User",
                email,
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}