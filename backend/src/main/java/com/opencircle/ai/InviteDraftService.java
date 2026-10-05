package com.opencircle.ai;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Turns a casual sentence into a structured invite. Gemini does it when available; otherwise simple rules do.
@Service
class InviteDraftService {

    static final int MAX_CONTENT_LENGTH = 500;
    static final int MAX_TAGS = 5;
    static final int MAX_TAG_LENGTH = 30;
    private static final int MAX_CAPACITY = 50;
    private static final int DEFAULT_GROUP_CAPACITY = 3;

    private static final Pattern TAG_PATTERN = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}_-]*");
    private static final Pattern COUNT_PATTERN =
            Pattern.compile("(\\d+)\\s*(people|persons|friends|others|ppl|players|students|more)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SINGLE_PATTERN =
            Pattern.compile("\\b(someone|a buddy|one person|a partner|a friend|1 person)\\b", Pattern.CASE_INSENSITIVE);

    // Topic keywords, matching the suggested topics on the new-invite page.
    private static final List<String[]> TOPIC_KEYWORDS = List.of(
            new String[]{"walk", "walk|stroll"},
            new String[]{"coffee", "coffee|latte|cafe|café|tea"},
            new String[]{"food", "lunch|dinner|eat|food|ramen|pizza|boba|brunch|snack"},
            new String[]{"study", "study|homework|exam|midterm|cram|library|assignment"},
            new String[]{"code", "code|coding|hackathon|java|python|programming|leetcode"},
            new String[]{"fitness", "gym|run|workout|fitness|hike|yoga|lift"},
            new String[]{"music", "music|concert|jam|guitar|band|karaoke"},
            new String[]{"games", "game|games|gaming|soccer|basketball|volleyball|chess|pickup"}
    );

    private final AiClient aiClient;
    private final AiRateLimiter rateLimiter;
    private final AiJson json;

    InviteDraftService(AiClient aiClient, AiRateLimiter rateLimiter, AiJson json) {
        this.aiClient = aiClient;
        this.rateLimiter = rateLimiter;
        this.json = json;
    }

    InviteDraftResponse draft(UUID userId, String text) {
        String input = text.trim();

        if (aiClient.isEnabled()) {
            rateLimiter.require(userId);

            try {
                return fromModel(input);
            } catch (AiUnavailableException exception) {
                // Fall through to the rules below.
            }
        }

        return fallback(input);
    }

    private InviteDraftResponse fromModel(String input) {
        JsonNode answer = json.parse(aiClient.generateJson(prompt(input), 0.3));

        String inviteType = "GROUP".equalsIgnoreCase(AiJson.text(answer, "inviteType")) ? "GROUP" : "SINGLE";
        Integer capacity = inviteType.equals("GROUP") ? clampCapacity(AiJson.integer(answer, "totalCapacity")) : null;

        List<String> rawTags = new ArrayList<>();
        JsonNode tags = answer.get("tags");
        if (tags != null && tags.isArray()) {
            tags.forEach(tag -> {
                if (tag.isString()) {
                    rawTags.add(tag.asString());
                }
            });
        }

        String content = tidy(AiJson.text(answer, "content"));

        return new InviteDraftResponse(
                content.isEmpty() ? tidy(input) : content,
                inviteType,
                capacity,
                normalizeTags(rawTags),
                true
        );
    }

    private String prompt(String input) {
        return """
                You turn a student's casual idea for meeting people into a structured invite for a campus meetup app.
                Return JSON: {"content": the invite rewritten as one friendly, clear message (max 400 characters, keep their meaning, no hashtags),
                "inviteType": "SINGLE" if they want exactly one other person, otherwise "GROUP",
                "totalCapacity": for GROUP, how many OTHER people they want (integer 2 to 50, 3 if unclear), null for SINGLE,
                "tags": up to 5 short lowercase single-word topics, preferring walk, coffee, food, study, code, fitness, music, games}.
                Idea: \"\"\"%s\"\"\"
                """.formatted(input);
    }

    static InviteDraftResponse fallback(String input) {
        Matcher count = COUNT_PATTERN.matcher(input);
        Integer capacity = null;
        String inviteType;

        if (count.find()) {
            int spots = Integer.parseInt(count.group(1));
            capacity = clampCapacity(spots);
            inviteType = spots <= 1 ? "SINGLE" : "GROUP";
            if (inviteType.equals("SINGLE")) {
                capacity = null;
            }
        } else if (SINGLE_PATTERN.matcher(input).find()) {
            inviteType = "SINGLE";
        } else {
            inviteType = "GROUP";
            capacity = DEFAULT_GROUP_CAPACITY;
        }

        List<String> tags = new ArrayList<>();
        String lower = input.toLowerCase(Locale.ROOT);
        for (String[] topic : TOPIC_KEYWORDS) {
            if (Pattern.compile("\\b(" + topic[1] + ")\\b").matcher(lower).find()) {
                tags.add(topic[0]);
            }
        }

        return new InviteDraftResponse(tidy(input), inviteType, capacity, normalizeTags(tags), false);
    }

    // Collapses whitespace, capitalises the first letter, and keeps within the invite length limit.
    static String tidy(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim().replaceAll("\\s+", " ");

        if (cleaned.isEmpty()) {
            return "";
        }

        cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);

        return cleaned.length() > MAX_CONTENT_LENGTH ? cleaned.substring(0, MAX_CONTENT_LENGTH).trim() : cleaned;
    }

    // The same shape the backend stores for invite topics: lowercase, no '#', words joined with '-'.
    static List<String> normalizeTags(List<String> values) {
        Set<String> tags = new LinkedHashSet<>();

        for (String value : values) {
            if (value == null) {
                continue;
            }

            String tag = value.trim().replaceFirst("^#+", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");

            if (tag.isEmpty() || tag.length() > MAX_TAG_LENGTH || !TAG_PATTERN.matcher(tag).matches()) {
                continue;
            }

            tags.add(tag);

            if (tags.size() == MAX_TAGS) {
                break;
            }
        }

        return List.copyOf(tags);
    }

    private static Integer clampCapacity(Integer value) {
        if (value == null) {
            return DEFAULT_GROUP_CAPACITY;
        }

        return Math.max(2, Math.min(MAX_CAPACITY, value));
    }
}
