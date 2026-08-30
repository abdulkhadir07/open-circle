package com.opencircle.invitepost;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitePostRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InvitePostRepository invitePosts;

    @Autowired
    private UserService userService;

    @Test
    void countryFeedReturnsOnlyActiveCountryPostsInSameCountry() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");

        InvitePost usaPost = invitePosts.save(post(
                "usa.repository@example.com",
                "+14155550500",
                "USA country post",
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "gambia.repository@example.com",
                "+220700000001",
                "Gambia country post",
                LocationScope.COUNTRY,
                "Banjul",
                null,
                "The Gambia",
                now
        ));

        invitePosts.save(post(
                "global.repository@example.com",
                "+14155550501",
                "Global post",
                LocationScope.GLOBAL,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "expired.repository@example.com",
                "+14155550502",
                "Expired USA post",
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                // Created 25 hours ago, so the post is one hour past its 24-hour expiration window.
                now.minusSeconds(25 * 60 * 60L)
        ));

        assertThat(invitePosts.findCountryFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.COUNTRY,
                "USA"
        ))
                .extracting(InvitePost::getContent)
                .containsExactly(usaPost.getContent());
    }

    @Test
    void stateRegionFeedReturnsOnlyPostsInSameStateRegion() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");

        InvitePost californiaPost = invitePosts.save(post(
                "california.repository@example.com",
                "+14155550503",
                "California post",
                LocationScope.STATE_REGION,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "nevada.repository@example.com",
                "+14155550504",
                "Nevada post",
                LocationScope.STATE_REGION,
                "Las Vegas",
                "Nevada",
                "USA",
                now
        ));

        assertThat(invitePosts.findStateRegionFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.STATE_REGION,
                "USA",
                "California"
        ))
                .extracting(InvitePost::getContent)
                .containsExactly(californiaPost.getContent());
    }

    @Test
    void cityFeedReturnsOnlyPostsInSameCityAndCountry() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");

        InvitePost sanFranciscoPost = invitePosts.save(post(
                "sf.repository@example.com",
                "+14155550505",
                "SF post",
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "la.repository@example.com",
                "+14155550506",
                "LA post",
                LocationScope.CITY,
                "Los Angeles",
                "California",
                "USA",
                now
        ));

        assertThat(invitePosts.findCityFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.CITY,
                "USA",
                "San Francisco"
        ))
                .extracting(InvitePost::getContent)
                .containsExactly(sanFranciscoPost.getContent());
    }

    @Test
    void globalFeedReturnsOnlyActiveGlobalPosts() {
        Instant now = Instant.parse("2026-08-29T12:00:00Z");

        InvitePost globalPost = invitePosts.save(post(
                "world.repository@example.com",
                "+14155550507",
                "Global post",
                LocationScope.GLOBAL,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "local.repository@example.com",
                "+14155550508",
                "Local post",
                LocationScope.COUNTRY,
                "San Francisco",
                "California",
                "USA",
                now
        ));

        invitePosts.save(post(
                "expired-global.repository@example.com",
                "+14155550509",
                "Expired global post",
                LocationScope.GLOBAL,
                "San Francisco",
                "California",
                "USA",
                now.minusSeconds(25 * 60 * 60L)
        ));

        assertThat(invitePosts.findGlobalFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.GLOBAL
        ))
                .extracting(InvitePost::getContent)
                .containsExactly(globalPost.getContent());
    }

    private InvitePost post(
            String email,
            String phoneNumber,
            String content,
            LocationScope locationScope,
            String city,
            String stateRegion,
            String country,
            Instant createdAt
    ) {
        return new InvitePost(
                user(email, phoneNumber),
                content,
                InviteType.SINGLE,
                1,
                locationScope,
                city,
                stateRegion,
                country,
                createdAt
        );
    }

    private AppUser user(String email, String phoneNumber) {
        return userService.createUser(
                "Repository",
                "User",
                email,
                "hashed-password",
                phoneNumber,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                null,
                "USA"
        );
    }
}