package com.opencircle.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
class GeminiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final AiProperties properties;
    private final Clock clock;
    private final RestClient restClient;

    // A fresh builder with its own timeouts, not the shared one that the location client uses.
    @Autowired
    GeminiClient(AiProperties properties, Clock clock) {
        this(newBuilder(properties), properties, clock);
    }

    GeminiClient(RestClient.Builder builder, AiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    }

    private static RestClient.Builder newBuilder(AiProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder().requestFactory(factory);
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public String generateJson(String prompt, double temperature) {
        if (!properties.isEnabled()) {
            throw new AiUnavailableException("AI is not configured");
        }

        Instant started = Instant.now(clock);
        RuntimeException lastFailure = null;

        for (String model : properties.getModels()) {
            if (Duration.between(started, Instant.now(clock)).compareTo(properties.getBudget()) > 0) {
                break;
            }

            try {
                String text = ask(model, prompt, temperature);
                if (text != null && !text.isBlank()) {
                    return text;
                }
                lastFailure = new AiUnavailableException("Empty answer from " + model);
            } catch (RestClientException exception) {
                // Never log the prompt or the key; the model name and failure type are enough.
                log.warn("[ai] {} failed: {}", model, exception.getClass().getSimpleName());
                lastFailure = exception;
            }
        }

        throw new AiUnavailableException("No model answered in time", lastFailure);
    }

    private String ask(String model, String prompt, double temperature) {
        GeminiResponse response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of(
                                "responseMimeType", "application/json",
                                "temperature", temperature
                        )
                ))
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }

        Candidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            return null;
        }

        return candidate.content().parts().get(0).text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Part(String text) {
    }
}
