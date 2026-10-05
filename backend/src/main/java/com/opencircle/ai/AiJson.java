package com.opencircle.ai;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Models sometimes wrap JSON in markdown fences or add stray text; this tolerates that and nothing more.
@Component
class AiJson {

    private final JsonMapper mapper;

    AiJson(JsonMapper mapper) {
        this.mapper = mapper;
    }

    JsonNode parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AiUnavailableException("Empty model output");
        }

        String text = raw.trim();

        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }

        try {
            return mapper.readTree(text);
        } catch (RuntimeException exception) {
            throw new AiUnavailableException("Model output was not valid JSON", exception);
        }
    }

    static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull() || !value.isString()) {
            return null;
        }

        String text = value.asString();
        return text == null || text.isBlank() ? null : text.trim();
    }

    static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || !value.isNumber()) {
            return null;
        }

        return value.asInt();
    }
}
