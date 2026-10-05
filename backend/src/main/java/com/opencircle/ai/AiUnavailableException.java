package com.opencircle.ai;

// Thrown when the model can't answer (no key, timeout, rate limited, bad output). Callers fall back to rules.
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
