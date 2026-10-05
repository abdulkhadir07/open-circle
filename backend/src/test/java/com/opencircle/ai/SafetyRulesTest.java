package com.opencircle.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Buy me a gift card and I'll meet you",
            "Send me 50 on venmo and we can hang out",
            "Guaranteed crypto profit if you join us",
            "I will find you after class",
            "Meet me behind the gym after dark, come alone",
            "don't tell anyone where we're going",
            "send me nudes",
            "Looking for a sugar daddy"
    })
    void blocksClearlyUnsafeText(String text) {
        SafetyVerdict verdict = SafetyRules.check(text);

        assertThat(verdict.ok()).as(text).isFalse();
        assertThat(verdict.reason()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Anyone want to grab coffee at the student center at 3?",
            "Studying for the Java midterm in the library, 2 spots left",
            "Pickup soccer on the quad tonight",
            "I can send you my notes after class",
            "Bring a gift for the birthday picnic",
            "Walk and talk around campus"
    })
    void allowsNormalCampusPlans(String text) {
        assertThat(SafetyRules.check(text).ok()).as(text).isTrue();
    }

    @Test
    void blankTextIsFine() {
        assertThat(SafetyRules.check(null).ok()).isTrue();
        assertThat(SafetyRules.check("   ").ok()).isTrue();
    }
}
