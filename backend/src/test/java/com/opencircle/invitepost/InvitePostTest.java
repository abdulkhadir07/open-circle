package com.opencircle.invitepost;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitePostTest {

    @Test
    void constructorCreatesSingleInviteWithOneCapacityAndExpiration() {
        Instant createdAt = Instant.parse("2026-08-29T12:00:00Z");

        InvitePost post = new InvitePost(
                user(),
                "Want to grab coffee?",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                createdAt
        );

        assertThat(post.getContent()).isEqualTo("Want to grab coffee?");
        assertThat(post.getInviteType()).isEqualTo(InviteType.SINGLE);
        assertThat(post.getTotalCapacity()).isEqualTo(1);
        assertThat(post.getAcceptedCount()).isZero();
        assertThat(post.getInvitesLeft()).isEqualTo(1);
        assertThat(post.getLocationScope()).isEqualTo(LocationScope.CITY);
        assertThat(post.getCity()).isEqualTo("San Francisco");
        assertThat(post.getStateRegion()).isEqualTo("California");
        assertThat(post.getCountry()).isEqualTo("USA");
        assertThat(post.getStatus()).isEqualTo(InvitePostStatus.ACTIVE);
        assertThat(post.getExpiresAt()).isEqualTo(Instant.parse("2026-08-30T12:00:00Z"));
    }

    @Test
    void constructorCreatesGroupInviteWithMultipleCapacity() {
        InvitePost post = new InvitePost(
                user(),
                "Beach hangout this weekend",
                InviteType.GROUP,
                5,
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        assertThat(post.getInviteType()).isEqualTo(InviteType.GROUP);
        assertThat(post.getTotalCapacity()).isEqualTo(5);
        assertThat(post.getInvitesLeft()).isEqualTo(5);
    }

    @Test
    void constructorRejectsBlankContent() {
        assertThatThrownBy(() -> new InvitePost(
                user(),
                " ",
                InviteType.SINGLE,
                1,
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content is required");
    }

    @Test
    void constructorRejectsInvalidSingleCapacity() {
        assertThatThrownBy(() -> new InvitePost(
                user(),
                "Coffee?",
                InviteType.SINGLE,
                3,
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Single invites must have a capacity of 1");
    }

    @Test
    void constructorRejectsGroupCapacityLessThanTwo() {
        assertThatThrownBy(() -> new InvitePost(
                user(),
                "Group hangout",
                InviteType.GROUP,
                1,
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group invites must have a capacity of at least 2");
    }

    @Test
    void constructorRejectsStateScopeWithoutStateRegion() {
        assertThatThrownBy(() -> new InvitePost(
                user(),
                "Anyone nearby?",
                InviteType.SINGLE,
                1,
                LocationScope.STATE_REGION,
                "Banjul",
                null,
                "The Gambia",
                Instant.parse("2026-08-29T12:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("State/region is required for state-region scoped posts");
    }

    @Test
    void isOpenReturnsFalseWhenPostIsExpired() {
        InvitePost post = new InvitePost(
                user(),
                "Coffee?",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        assertThat(post.isOpen(Instant.parse("2026-08-30T12:00:00Z"))).isFalse();
    }

    @Test
    void closeStopsPostFromBeingOpen() {
        InvitePost post = new InvitePost(
                user(),
                "Coffee?",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        post.close();

        assertThat(post.getStatus()).isEqualTo(InvitePostStatus.CLOSED);
        assertThat(post.isOpen(Instant.parse("2026-08-29T13:00:00Z"))).isFalse();
    }

    @Test
    void recordAcceptedEngagementIncreasesAcceptedCount() {
        InvitePost post = new InvitePost(
                user(),
                "Beach hangout",
                InviteType.GROUP,
                2,
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        post.recordAcceptedEngagement();

        assertThat(post.getAcceptedCount()).isEqualTo(1);
        assertThat(post.getInvitesLeft()).isEqualTo(1);
    }

    @Test
    void recordAcceptedEngagementRejectsWhenPostIsFull() {
        InvitePost post = new InvitePost(
                user(),
                "Coffee?",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        post.recordAcceptedEngagement();

        assertThatThrownBy(post::recordAcceptedEngagement)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invite post is already full");
    }

    private AppUser user() {
        return new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}