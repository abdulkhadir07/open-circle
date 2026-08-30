package com.opencircle.invitepost;

import com.opencircle.location.LocationNotVerifiedException;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvitePostServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-30T12:00:00Z");

    private final InvitePostRepository posts = mock(InvitePostRepository.class);
    private final InvitePostService service = new InvitePostService(
            posts,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createPostUsesVerifiedLocationSnapshot() {
        AppUser poster = verifiedUser("poster@example.com", "San Francisco", "California", "USA");
        when(posts.save(any(InvitePost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "Anyone want to grab coffee near campus?",
                InviteType.GROUP,
                4,
                LocationScope.CITY
        );

        InvitePost post = service.createPost(poster, request);

        assertThat(post.getPoster()).isEqualTo(poster);
        assertThat(post.getContent()).isEqualTo("Anyone want to grab coffee near campus?");
        assertThat(post.getInviteType()).isEqualTo(InviteType.GROUP);
        assertThat(post.getTotalCapacity()).isEqualTo(4);
        assertThat(post.getLocationScope()).isEqualTo(LocationScope.CITY);
        assertThat(post.getCity()).isEqualTo("San Francisco");
        assertThat(post.getStateRegion()).isEqualTo("California");
        assertThat(post.getCountry()).isEqualTo("USA");
        assertThat(post.getCreatedAt()).isEqualTo(NOW);
        assertThat(post.getExpiresAt()).isEqualTo(NOW.plusSeconds(24 * 60 * 60L));

        verify(posts).save(post);
    }

    @Test
    void createPostDefaultsSingleInviteCapacityToOne() {
        AppUser poster = verifiedUser("single@example.com", "San Francisco", "California", "USA");
        when(posts.save(any(InvitePost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "One person want to study Java?",
                InviteType.SINGLE,
                null,
                LocationScope.COUNTRY
        );

        InvitePost post = service.createPost(poster, request);

        assertThat(post.getInviteType()).isEqualTo(InviteType.SINGLE);
        assertThat(post.getTotalCapacity()).isEqualTo(1);
        assertThat(post.getInvitesLeft()).isEqualTo(1);
    }

    @Test
    void createPostRejectsUnverifiedLocation() {
        AppUser poster = user("unverified@example.com", "San Francisco", "California", "USA");

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "This should not post yet",
                InviteType.SINGLE,
                1,
                LocationScope.GLOBAL
        );

        assertThatThrownBy(() -> service.createPost(poster, request))
                .isInstanceOf(LocationNotVerifiedException.class);

        verify(posts, never()).save(any());
    }

    @Test
    void createPostRejectsInvalidSingleCapacity() {
        AppUser poster = verifiedUser("bad.single@example.com", "San Francisco", "California", "USA");

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "Single invite with too many spots",
                InviteType.SINGLE,
                2,
                LocationScope.CITY
        );

        assertThatThrownBy(() -> service.createPost(poster, request))
                .isInstanceOf(InvalidInvitePostRequestException.class)
                .hasMessage("Single invites must have a total capacity of 1");

        verify(posts, never()).save(any());
    }

    @Test
    void createPostRejectsInvalidGroupCapacity() {
        AppUser poster = verifiedUser("bad.group@example.com", "San Francisco", "California", "USA");

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "Group invite with one spot",
                InviteType.GROUP,
                1,
                LocationScope.CITY
        );

        assertThatThrownBy(() -> service.createPost(poster, request))
                .isInstanceOf(InvalidInvitePostRequestException.class)
                .hasMessage("Group invites must have a total capacity of at least 2");

        verify(posts, never()).save(any());
    }

    @Test
    void createPostRejectsStateRegionScopeWhenVerifiedLocationHasNoStateRegion() {
        AppUser poster = verifiedUser("banjul@example.com", "Banjul", null, "The Gambia");

        CreateInvitePostRequest request = new CreateInvitePostRequest(
                "Anyone in my region?",
                InviteType.GROUP,
                3,
                LocationScope.STATE_REGION
        );

        assertThatThrownBy(() -> service.createPost(poster, request))
                .isInstanceOf(InvalidInvitePostRequestException.class)
                .hasMessage("State/region scope is not available for your verified location");

        verify(posts, never()).save(any());
    }

    @Test
    void defaultLocalFeedCombinesMatchingCountryStateAndCityPostsNewestFirst() {
        AppUser viewer = verifiedUser("viewer@example.com", "San Francisco", "California", "USA");

        InvitePost countryPost = post(viewer, "Country post", LocationScope.COUNTRY, NOW.minusSeconds(300));
        InvitePost statePost = post(viewer, "State post", LocationScope.STATE_REGION, NOW.minusSeconds(100));
        InvitePost cityPost = post(viewer, "City post", LocationScope.CITY, NOW.minusSeconds(200));

        when(posts.findCountryFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.COUNTRY, "USA"))
                .thenReturn(List.of(countryPost));
        when(posts.findStateRegionFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.STATE_REGION, "USA", "California"))
                .thenReturn(List.of(statePost));
        when(posts.findCityFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.CITY, "USA", "San Francisco"))
                .thenReturn(List.of(cityPost));

        // The default local feed includes all matching local scopes, then sorts them newest first.
        List<InvitePost> feed = service.getLocalFeed(viewer, null);

        assertThat(feed).containsExactly(statePost, cityPost, countryPost);
        verify(posts, never()).findGlobalFeed(any(), any(), any());
    }

    @Test
    void defaultLocalFeedSkipsStateRegionWhenViewerHasNoStateRegion() {
        AppUser viewer = verifiedUser("gambia.viewer@example.com", "Banjul", null, "The Gambia");

        InvitePost countryPost = post(viewer, "Country post", LocationScope.COUNTRY, NOW.minusSeconds(200));
        InvitePost cityPost = post(viewer, "City post", LocationScope.CITY, NOW.minusSeconds(100));

        when(posts.findCountryFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.COUNTRY, "The Gambia"))
                .thenReturn(List.of(countryPost));
        when(posts.findCityFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.CITY, "The Gambia", "Banjul"))
                .thenReturn(List.of(cityPost));

        List<InvitePost> feed = service.getLocalFeed(viewer, null);

        assertThat(feed).containsExactly(cityPost, countryPost);
        verify(posts, never()).findStateRegionFeed(any(), any(), any(), any(), any());
    }

    @Test
    void localFeedRejectsGlobalScope() {
        AppUser viewer = verifiedUser("local.global@example.com", "San Francisco", "California", "USA");

        assertThatThrownBy(() -> service.getLocalFeed(viewer, LocationScope.GLOBAL))
                .isInstanceOf(InvalidInvitePostRequestException.class)
                .hasMessage("Global scope is not part of the local feed");
    }

    @Test
    void globalFeedRejectsUnverifiedLocation() {
        AppUser viewer = user("global.unverified@example.com", "Banjul", null, "The Gambia");

        assertThatThrownBy(() -> service.getGlobalFeed(viewer))
                .isInstanceOf(LocationNotVerifiedException.class);

        verify(posts, never()).findGlobalFeed(any(), any(), any());
    }

    @Test
    void globalFeedReturnsOnlyGlobalPosts() {
        AppUser poster = verifiedUser("global@example.com", "Banjul", null, "The Gambia");
        InvitePost globalPost = post(poster, "Global post", LocationScope.GLOBAL, NOW.minusSeconds(100));

        when(posts.findGlobalFeed(InvitePostStatus.ACTIVE, NOW, LocationScope.GLOBAL))
                .thenReturn(List.of(globalPost));

        List<InvitePost> feed = service.getGlobalFeed(poster);

        assertThat(feed).containsExactly(globalPost);
    }

    private AppUser verifiedUser(String email, String city, String stateRegion, String country) {
        AppUser user = user(email, city, stateRegion, country);
        user.verifyLocation(city, stateRegion, country, NOW.minusSeconds(60));
        return user;
    }

    private AppUser user(String email, String city, String stateRegion, String country) {
        return new AppUser(
                email.substring(0, email.indexOf("@")).replace(".", "_") + "_1234",
                "Test",
                "User",
                email,
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                city,
                stateRegion,
                country
        );
    }

    private InvitePost post(
            AppUser poster,
            String content,
            LocationScope scope,
            Instant createdAt
    ) {
        return new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                scope,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                createdAt
        );
    }
}