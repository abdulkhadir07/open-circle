package com.opencircle.ai;

import java.util.List;
import java.util.regex.Pattern;

// Deterministic hard rules. They always apply, with or without the AI check, and never depend on quota.
final class SafetyRules {

    private record Rule(Pattern pattern, String reason) {

        static Rule of(String regex, String reason) {
            return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), reason);
        }
    }

    private static final List<Rule> RULES = List.of(
            Rule.of("\\bgift\\s*cards?\\b|\\bwestern\\s+union\\b|\\bwire\\s+(me|the)\\s+(money|funds)\\b",
                    "That looks like a request for money or gift cards. Please keep payments out of OpenCircle."),
            Rule.of("\\b(send|give|lend)\\s+(me\\s+)?\\$?\\d+\\b.*\\b(venmo|cash\\s*app|cashapp|zelle|paypal)\\b"
                            + "|\\b(venmo|cash\\s*app|cashapp|zelle|paypal)\\b.*\\b(send|pay)\\b.*\\$?\\d+",
                    "That looks like a request for money. Please keep payments out of OpenCircle."),
            Rule.of("\\b(bitcoin|crypto(currency)?|nft)\\b.*\\b(invest|double|guaranteed|returns?|profit)\\b"
                            + "|\\b(invest|guaranteed|profit)\\b.*\\b(bitcoin|crypto(currency)?)\\b",
                    "That looks like an investment or crypto pitch. Those aren't allowed here."),
            Rule.of("\\b(kill|hurt|stab|shoot|beat\\s+up|murder)\\s+(you|him|her|them|yourself|myself)\\b"
                            + "|\\bi('| wi)?ll\\s+(find|hurt|kill)\\s+you\\b|\\bkys\\b",
                    "That reads like a threat or encouragement of harm. Please rephrase."),
            Rule.of("\\bcome\\s+alone\\b|\\bdon'?t\\s+tell\\s+anyone\\b|\\bafter\\s+dark\\b.*\\b(alone|behind)\\b"
                            + "|\\bbehind\\s+the\\s+\\w+\\b.*\\balone\\b",
                    "That sounds like an unsafe way to meet. Suggest a public spot on campus instead."),
            Rule.of("\\b(send|share)\\s+(me\\s+)?(nudes?|nude\\s+pics?|explicit\\s+pics?)\\b"
                            + "|\\bsugar\\s+(daddy|baby|mama)\\b|\\bhook\\s?up\\s+for\\s+(money|cash)\\b",
                    "That looks like sexual solicitation, which isn't allowed here.")
    );

    private SafetyRules() {
    }

    static SafetyVerdict check(String text) {
        if (text == null || text.isBlank()) {
            return SafetyVerdict.OK;
        }

        for (Rule rule : RULES) {
            if (rule.pattern().matcher(text).find()) {
                return SafetyVerdict.blocked(rule.reason());
            }
        }

        return SafetyVerdict.OK;
    }
}
