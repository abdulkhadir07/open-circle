package com.opencircle.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    // Optional: with no key every AI feature falls back to simple built-in rules.
    private String apiKey;

    private String baseUrl = "https://generativelanguage.googleapis.com";

    // Tried in order; the next model answers when one is rate limited or overloaded.
    private List<String> models = List.of(
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration readTimeout = Duration.ofSeconds(5);

    // Total time one request may spend walking the model chain before giving up.
    private Duration budget = Duration.ofSeconds(6);

    private int requestsPerMinute = 20;

    private Duration feedInsightsCacheTtl = Duration.ofMinutes(5);

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> getModels() {
        return models;
    }

    public void setModels(List<String> models) {
        this.models = models;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Duration getBudget() {
        return budget;
    }

    public void setBudget(Duration budget) {
        this.budget = budget;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public Duration getFeedInsightsCacheTtl() {
        return feedInsightsCacheTtl;
    }

    public void setFeedInsightsCacheTtl(Duration feedInsightsCacheTtl) {
        this.feedInsightsCacheTtl = feedInsightsCacheTtl;
    }
}
