package com.opencircle.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private static final String BASE = "https://gemini.test";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private AiProperties properties(String key, String... models) {
        AiProperties properties = new AiProperties();
        properties.setApiKey(key);
        properties.setBaseUrl(BASE);
        properties.setModels(List.of(models));
        return properties;
    }

    private GeminiClient client(AiProperties properties, Clock clock) {
        return new GeminiClient(builder, properties, clock);
    }

    private static String answer(String text) {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" + quote(text) + "}]}}]}";
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    void returnsTheModelsAnswerAndSendsTheKeyOnlyInAHeader() {
        server.expect(requestTo(BASE + "/v1beta/models/model-a:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "secret-key"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("say hi"))
                .andRespond(withSuccess(answer("{\"ok\":true}"), MediaType.APPLICATION_JSON));

        GeminiClient client = client(properties("secret-key", "model-a"), Clock.systemUTC());

        assertThat(client.generateJson("say hi", 0.2)).isEqualTo("{\"ok\":true}");
        server.verify();
    }

    @Test
    void movesToTheNextModelWhenOneIsOverloaded() {
        server.expect(requestTo(BASE + "/v1beta/models/model-a:generateContent"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(BASE + "/v1beta/models/model-b:generateContent"))
                .andRespond(withSuccess(answer("second"), MediaType.APPLICATION_JSON));

        GeminiClient client = client(properties("k", "model-a", "model-b"), Clock.systemUTC());

        assertThat(client.generateJson("prompt", 0.2)).isEqualTo("second");
        server.verify();
    }

    @Test
    void movesOnWhenAModelAnswersWithNothing() {
        server.expect(requestTo(BASE + "/v1beta/models/model-a:generateContent"))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "/v1beta/models/model-b:generateContent"))
                .andRespond(withSuccess(answer("real answer"), MediaType.APPLICATION_JSON));

        GeminiClient client = client(properties("k", "model-a", "model-b"), Clock.systemUTC());

        assertThat(client.generateJson("prompt", 0.2)).isEqualTo("real answer");
    }

    @Test
    void givesUpWhenEveryModelFails() {
        server.expect(requestTo(BASE + "/v1beta/models/model-a:generateContent")).andRespond(withServerError());
        server.expect(requestTo(BASE + "/v1beta/models/model-b:generateContent")).andRespond(withServerError());

        GeminiClient client = client(properties("k", "model-a", "model-b"), Clock.systemUTC());

        assertThatThrownBy(() -> client.generateJson("prompt", 0.2))
                .isInstanceOf(AiUnavailableException.class);
        server.verify();
    }

    @Test
    void doesNotCallAnythingWithoutAKey() {
        GeminiClient client = client(properties("", "model-a"), Clock.systemUTC());

        assertThat(client.isEnabled()).isFalse();
        assertThatThrownBy(() -> client.generateJson("prompt", 0.2))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessage("AI is not configured");
        server.verify();
    }

    @Test
    void stopsTryingModelsOnceTheTimeBudgetIsSpent() {
        AdvancingClock clock = new AdvancingClock(Duration.ofSeconds(10));
        AiProperties properties = properties("k", "model-a", "model-b");
        properties.setBudget(Duration.ofSeconds(6));

        // Only the first model is tried: after it fails, ten seconds have passed and the budget is gone.
        server.expect(requestTo(BASE + "/v1beta/models/model-a:generateContent")).andRespond(withServerError());

        GeminiClient client = client(properties, clock);

        assertThatThrownBy(() -> client.generateJson("prompt", 0.2)).isInstanceOf(AiUnavailableException.class);
        server.verify();
    }

    // A clock that jumps forward every time it is read, to simulate a slow model.
    private static final class AdvancingClock extends Clock {

        private final Duration step;
        private Instant now = Instant.parse("2026-10-04T12:00:00Z");
        private int reads = 0;

        AdvancingClock(Duration step) {
            this.step = step;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            // Reads 1 and 2 are the start time and the budget check before the first model, so no time
            // has passed yet. Every later read happens after that first call "took" a while.
            reads++;
            if (reads > 2) {
                now = now.plus(step);
            }
            return now;
        }
    }
}
