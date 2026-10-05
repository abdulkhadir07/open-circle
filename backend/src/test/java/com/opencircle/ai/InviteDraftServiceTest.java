package com.opencircle.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InviteDraftServiceTest {

    private final AiClient aiClient = mock(AiClient.class);
    private final AiProperties properties = new AiProperties();
    private final AiRateLimiter limiter = new AiRateLimiter(properties, Clock.systemUTC());
    private final InviteDraftService service =
            new InviteDraftService(aiClient, limiter, new AiJson(JsonMapper.builder().build()));
    private final UUID user = UUID.randomUUID();

    @Test
    void usesTheModelsStructuredAnswerWhenAvailable() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("""
                {"content": "grab coffee at the student center around 3?", "inviteType": "GROUP",
                 "totalCapacity": 4, "tags": ["Coffee", "#chat", "coffee"]}
                """);

        InviteDraftResponse draft = service.draft(user, "coffee at 3 anyone");

        assertThat(draft.aiGenerated()).isTrue();
        assertThat(draft.content()).isEqualTo("Grab coffee at the student center around 3?");
        assertThat(draft.inviteType()).isEqualTo("GROUP");
        assertThat(draft.totalCapacity()).isEqualTo(4);
        assertThat(draft.tags()).containsExactly("coffee", "chat");
    }

    @Test
    void cleansUpAnythingOddInTheModelsAnswer() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("""
                {"content": "  hi  ", "inviteType": "single", "totalCapacity": 9,
                 "tags": ["a b", "x", "too many", "four", "five", "six", "seven", "bad!tag"]}
                """);

        InviteDraftResponse draft = service.draft(user, "hi");

        assertThat(draft.inviteType()).isEqualTo("SINGLE");
        assertThat(draft.totalCapacity()).isNull();
        assertThat(draft.tags()).hasSize(5).doesNotContain("bad!tag");
        assertThat(draft.content()).isEqualTo("Hi");
    }

    @Test
    void clampsAGroupSizeIntoTheValidRange() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"content\": \"x\", \"inviteType\": \"GROUP\", \"totalCapacity\": 500, \"tags\": []}")
                .thenReturn("{\"content\": \"x\", \"inviteType\": \"GROUP\", \"totalCapacity\": 1, \"tags\": []}")
                .thenReturn("{\"content\": \"x\", \"inviteType\": \"GROUP\", \"tags\": []}");

        assertThat(service.draft(user, "x").totalCapacity()).isEqualTo(50);
        assertThat(service.draft(user, "x").totalCapacity()).isEqualTo(2);
        assertThat(service.draft(user, "x").totalCapacity()).isEqualTo(3);
    }

    @Test
    void keepsTheOriginalTextWhenTheModelReturnsNone() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"inviteType\": \"SINGLE\", \"tags\": []}");

        assertThat(service.draft(user, "study for the java midterm").content())
                .isEqualTo("Study for the java midterm");
    }

    @Test
    void fallsBackToRulesWhenTheModelFails() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenThrow(new AiUnavailableException("down"));

        InviteDraftResponse draft = service.draft(user, "need 4 players for pickup soccer tonight");

        assertThat(draft.aiGenerated()).isFalse();
        assertThat(draft.inviteType()).isEqualTo("GROUP");
        assertThat(draft.totalCapacity()).isEqualTo(4);
        assertThat(draft.tags()).contains("games");
    }

    @Test
    void usesRulesAndNeverCallsTheModelWithNoKey() {
        when(aiClient.isEnabled()).thenReturn(false);

        InviteDraftResponse draft = service.draft(user, "anyone want to get coffee");

        assertThat(draft.aiGenerated()).isFalse();
        assertThat(draft.tags()).containsExactly("coffee");
        verify(aiClient, never()).generateJson(anyString(), anyDouble());
    }

    @Test
    void refusesWhenTheUserHasUsedTheirAiAllowance() {
        properties.setRequestsPerMinute(1);
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"content\": \"x\", \"inviteType\": \"SINGLE\", \"tags\": []}");

        service.draft(user, "first");

        assertThatThrownBy(() -> service.draft(user, "second")).isInstanceOf(TooManyAiRequestsException.class);
    }

    @Test
    void ruleBasedDraftReadsSizeAndSingleWording() {
        assertThat(InviteDraftService.fallback("Looking for someone to study with").inviteType()).isEqualTo("SINGLE");
        assertThat(InviteDraftService.fallback("Looking for someone to study with").totalCapacity()).isNull();

        InviteDraftResponse oneSpot = InviteDraftService.fallback("Need 1 person for the gym");
        assertThat(oneSpot.inviteType()).isEqualTo("SINGLE");

        InviteDraftResponse unspecified = InviteDraftService.fallback("Walk around campus after class");
        assertThat(unspecified.inviteType()).isEqualTo("GROUP");
        assertThat(unspecified.totalCapacity()).isEqualTo(3);
        assertThat(unspecified.tags()).containsExactly("walk");

        assertThat(InviteDraftService.fallback("2 more ppl for a java study session").totalCapacity()).isEqualTo(2);
    }

    @Test
    void normalizesTagsTheWayInvitesStoreThem() {
        assertThat(InviteDraftService.normalizeTags(Arrays.asList("  #Board Games ", null, "", "x".repeat(31), "walk", "WALK")))
                .containsExactly("board-games", "walk");
        assertThat(InviteDraftService.normalizeTags(List.of("a", "b", "c", "d", "e", "f"))).hasSize(5);
    }

    @Test
    void tidyKeepsTextWithinTheInviteLimit() {
        assertThat(InviteDraftService.tidy("  hello   world  ")).isEqualTo("Hello world");
        assertThat(InviteDraftService.tidy("x".repeat(600))).hasSize(500);
        assertThat(InviteDraftService.tidy(null)).isEmpty();
    }
}
