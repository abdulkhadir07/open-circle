package com.opencircle.ai;

import com.opencircle.invitepost.CampusFeedQuery;
import com.opencircle.invitepost.FeedInvite;
import com.opencircle.user.AppUser;
import com.opencircle.user.ProfileInterests;
import com.opencircle.user.ProfileInterestsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedInsightsServiceTest {

    private final CampusFeedQuery feed = mock(CampusFeedQuery.class);
    private final ProfileInterestsQuery profiles = mock(ProfileInterestsQuery.class);
    private final AiClient aiClient = mock(AiClient.class);
    private final AiProperties properties = new AiProperties();
    private final FeedInsightsService service = new FeedInsightsService(
            feed,
            profiles,
            aiClient,
            new AiRateLimiter(properties, Clock.systemUTC()),
            new AiJson(JsonMapper.builder().build()),
            properties,
            Clock.systemUTC()
    );

    private final AppUser viewer = new AppUser(
            "viewer_1234", "Maya", "Chen", "maya@student.sfsu.edu", "hash", "+14155550123", LocalDate.of(2000, 1, 1));
    private final UUID viewerId = UUID.randomUUID();

    private FeedInvite invite(String content, List<String> tags) {
        return new FeedInvite(UUID.randomUUID(), UUID.randomUUID(), content, tags, "GROUP", 2);
    }

    private void givenFeed(FeedInvite... invites) {
        when(feed.openInvites(viewer, 20)).thenReturn(List.of(invites));
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(viewer, "id", viewerId);
        when(profiles.forUser(viewerId)).thenReturn(new ProfileInterests("I love hiking", List.of("coffee", "hiking")));
    }

    @Test
    void anEmptyFeedGetsAFriendlyNudgeWithoutAnyAiCall() {
        givenFeed();
        when(aiClient.isEnabled()).thenReturn(true);

        FeedInsightsResponse response = service.insights(viewer);

        assertThat(response.digest()).isEqualTo("Quiet day so far. Be the first to start something.");
        assertThat(response.reasons()).isEmpty();
        verify(aiClient, never()).generateJson(anyString(), anyDouble());
    }

    @Test
    void ownInvitesAreLeftOutOfTheDigest() {
        FeedInvite mine = new FeedInvite(UUID.randomUUID(), viewerId, "my own invite", List.of("coffee"), "GROUP", 2);
        givenFeed(mine);

        assertThat(service.insights(viewer).digest()).isEqualTo("Quiet day so far. Be the first to start something.");
    }

    @Test
    void withoutAKeyReasonsComeFromMatchingInterestsToTopicsAndText() {
        FeedInvite coffee = invite("Coffee chat at the student center", List.of("coffee"));
        FeedInvite hike = invite("Weekend trip to Mount Tam", List.of("outdoors"));
        FeedInvite unrelated = invite("Chess in the library", List.of("games"));
        givenFeed(coffee, hike, unrelated);
        when(aiClient.isEnabled()).thenReturn(false);

        FeedInsightsResponse response = service.insights(viewer);

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.reasons()).extracting(FeedReason::invitePostId).containsExactly(coffee.id());
        assertThat(response.reasons().get(0).reason()).isEqualTo("Matches your interest in coffee");
        assertThat(response.digest()).startsWith("3 open invites on your campus today.").contains("Coffee chat");
    }

    @Test
    void usesTheModelsDigestAndKeepsOnlyReasonsForRealInvites() {
        FeedInvite coffee = invite("Coffee chat", List.of("coffee"));
        givenFeed(coffee);
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("""
                {"digest": "A coffee chat is happening that suits you.",
                 "reasons": [{"id": "%s", "reason": "You like coffee"},
                             {"id": "%s", "reason": "made up"},
                             {"id": "not-a-uuid", "reason": "bad id"}]}
                """.formatted(coffee.id(), UUID.randomUUID()));

        FeedInsightsResponse response = service.insights(viewer);

        assertThat(response.aiGenerated()).isTrue();
        assertThat(response.digest()).isEqualTo("A coffee chat is happening that suits you.");
        assertThat(response.reasons()).containsExactly(new FeedReason(coffee.id(), "You like coffee"));
    }

    @Test
    void fallsBackToRulesWhenTheModelFails() {
        givenFeed(invite("Coffee chat", List.of("coffee")));
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenThrow(new AiUnavailableException("down"));

        FeedInsightsResponse response = service.insights(viewer);

        assertThat(response.aiGenerated()).isFalse();
        assertThat(response.reasons()).hasSize(1);
    }

    @Test
    void reusesTheAnswerForTheSameFeedInsteadOfAskingAgain() {
        givenFeed(invite("Coffee chat", List.of("coffee")));
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("{\"digest\": \"Nice day.\", \"reasons\": []}");

        service.insights(viewer);
        service.insights(viewer);

        verify(aiClient, times(1)).generateJson(anyString(), anyDouble());
    }

    @Test
    void overTheAiLimitQuietlyUsesTheSimpleVersion() {
        properties.setRequestsPerMinute(0);
        givenFeed(invite("Coffee chat", List.of("coffee")));
        when(aiClient.isEnabled()).thenReturn(true);

        FeedInsightsResponse response = service.insights(viewer);

        assertThat(response.aiGenerated()).isFalse();
        verify(aiClient, never()).generateJson(anyString(), anyDouble());
    }
}
