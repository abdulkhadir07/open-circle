package com.opencircle.ai;

public interface AiClient {

    // Sends a prompt and returns the model's JSON answer as text; throws AiUnavailableException on any failure.
    String generateJson(String prompt, double temperature);

    boolean isEnabled();
}
