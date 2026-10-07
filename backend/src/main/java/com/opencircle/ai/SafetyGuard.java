package com.opencircle.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

// Safety Guardian: screens content before it is saved. Hard rules always apply. The AI check is a bonus:
// if the model is down, slow, or the person is over their AI quota, clean text is still allowed through.
@Component
public class SafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuard.class);

    private static final int MAX_REASON_LENGTH = 160;

    private final AiClient aiClient;
    private final AiRateLimiter rateLimiter;
    private final AiJson json;

    SafetyGuard(AiClient aiClient, AiRateLimiter rateLimiter, AiJson json) {
        this.aiClient = aiClient;
        this.rateLimiter = rateLimiter;
        this.json = json;
    }

    // Call this from a controller, before the transactional service, so no database connection is held
    // while waiting for the model.
    public void requireSafe(UUID userId, String text, ContentKind kind) {
        SafetyVerdict verdict = SafetyRules.check(text);

        if (verdict.ok() && aiClient.isEnabled() && rateLimiter.tryAcquire(userId)) {
            verdict = askModel(text, kind);
        }

        if (!verdict.ok()) {
            throw new UnsafeContentException(verdict.reason());
        }
    }

    private SafetyVerdict askModel(String text, ContentKind kind) {
        try {
            JsonNode answer = json.parse(aiClient.generateJson(prompt(text, kind), 0.0));
            JsonNode ok = answer.get("ok");

            if (ok == null || !ok.isBoolean() || ok.asBoolean()) {
                return SafetyVerdict.OK;
            }

            String reason = AiJson.text(answer, "reason");
            if (reason == null) {
                reason = "This looks unsafe to post. Please rephrase it.";
            } else if (reason.length() > MAX_REASON_LENGTH) {
                reason = reason.substring(0, MAX_REASON_LENGTH - 1) + "…";
            }

            return SafetyVerdict.blocked(reason);
        } catch (AiUnavailableException exception) {
            log.debug("[ai] safety check skipped: {}", exception.getMessage());
            return SafetyVerdict.OK;
        }
    }

    private String prompt(String text, ContentKind kind) {
        return """
                You are "Safety Guardian" for a meetup app. Review this %s.
                Flag it (ok=false) only if it clearly contains: harassment or hate, sexual content, threats,
                scams or requests for money, gift cards or crypto, or pressure to meet somewhere isolated or secret.
                Casual language, slang, jokes and normal plans to meet in public are fine (ok=true).
                Return JSON: {"ok": boolean, "reason": a short, friendly sentence telling the author what to change, or null if ok}.
                Content: \"\"\"%s\"\"\"
                """.formatted(kind.label(), text);
    }
}
