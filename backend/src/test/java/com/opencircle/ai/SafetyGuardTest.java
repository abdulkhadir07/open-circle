package com.opencircle.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SafetyGuardTest {

    private final AiClient aiClient = mock(AiClient.class);
    private final AiProperties properties = new AiProperties();
    private final AiRateLimiter limiter = new AiRateLimiter(properties, Clock.systemUTC());
    private final SafetyGuard guard = new SafetyGuard(aiClient, limiter, new AiJson(JsonMapper.builder().build()));
    private final UUID user = UUID.randomUUID();

    @Test
    void hardRulesBlockWithoutAskingTheModel() {
        when(aiClient.isEnabled()).thenReturn(true);

        assertThatThrownBy(() -> guard.requireSafe(user, "Send me a gift card", ContentKind.MESSAGE))
                .isInstanceOf(UnsafeContentException.class)
                .satisfies(exception -> assertThat(((UnsafeContentException) exception).status().value()).isEqualTo(422));

        verify(aiClient, never()).generateJson(anyString(), anyDouble());
    }

    @Test
    void theModelCanBlockTextTheRulesAllow() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("{\"ok\": false, \"reason\": \"That reads as harassment. Please be kind.\"}");

        assertThatThrownBy(() -> guard.requireSafe(user, "you are the worst person ever", ContentKind.BANTER))
                .isInstanceOf(UnsafeContentException.class)
                .hasMessage("That reads as harassment. Please be kind.");
    }

    @Test
    void anAnswerWrappedInMarkdownFencesStillWorks() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble()))
                .thenReturn("```json\n{\"ok\": false, \"reason\": \"No threats.\"}\n```");

        assertThatThrownBy(() -> guard.requireSafe(user, "text", ContentKind.INVITE))
                .isInstanceOf(UnsafeContentException.class)
                .hasMessage("No threats.");
    }

    @Test
    void aBlockWithNoReasonGetsAFriendlyDefault() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("{\"ok\": false}");

        assertThatThrownBy(() -> guard.requireSafe(user, "text", ContentKind.INVITE))
                .isInstanceOf(UnsafeContentException.class)
                .hasMessage("This looks unsafe to post. Please rephrase it.");
    }

    @Test
    void cleanTextPassesWhenTheModelApproves() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("{\"ok\": true, \"reason\": null}");

        assertThatCode(() -> guard.requireSafe(user, "Coffee at 3?", ContentKind.INVITE)).doesNotThrowAnyException();
    }

    @Test
    void cleanTextPassesWhenTheModelIsDown() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenThrow(new AiUnavailableException("down"));

        assertThatCode(() -> guard.requireSafe(user, "Coffee at 3?", ContentKind.INVITE)).doesNotThrowAnyException();
    }

    @Test
    void cleanTextPassesWhenTheModelAnswersGibberish() {
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("not json at all");

        assertThatCode(() -> guard.requireSafe(user, "Coffee at 3?", ContentKind.INVITE)).doesNotThrowAnyException();
    }

    @Test
    void goingOverTheAiLimitSkipsTheModelButStillAppliesTheRules() {
        properties.setRequestsPerMinute(1);
        when(aiClient.isEnabled()).thenReturn(true);
        when(aiClient.generateJson(anyString(), anyDouble())).thenReturn("{\"ok\": true}");

        guard.requireSafe(user, "first", ContentKind.MESSAGE);
        // The second check is over the limit: it doesn't fail, it just skips the model...
        assertThatCode(() -> guard.requireSafe(user, "second, a normal message", ContentKind.MESSAGE))
                .doesNotThrowAnyException();
        // ...but the hard rules never depend on quota.
        assertThatThrownBy(() -> guard.requireSafe(user, "send me nudes", ContentKind.MESSAGE))
                .isInstanceOf(UnsafeContentException.class);
    }

    @Test
    void withNoKeyOnlyTheRulesRun() {
        when(aiClient.isEnabled()).thenReturn(false);

        assertThatCode(() -> guard.requireSafe(user, "Coffee at 3?", ContentKind.INVITE)).doesNotThrowAnyException();
        verify(aiClient, never()).generateJson(anyString(), anyDouble());
    }
}
