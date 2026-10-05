package com.opencircle.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiRateLimiterTest {

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-04T12:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
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
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final AiProperties properties = new AiProperties();
    private final AiRateLimiter limiter = new AiRateLimiter(properties, clock);

    @Test
    void allowsUpToTheLimitThenRefuses() {
        properties.setRequestsPerMinute(3);
        UUID user = UUID.randomUUID();

        assertThat(limiter.tryAcquire(user)).isTrue();
        assertThat(limiter.tryAcquire(user)).isTrue();
        assertThat(limiter.tryAcquire(user)).isTrue();
        assertThat(limiter.tryAcquire(user)).isFalse();
    }

    @Test
    void theWindowSlidesSoRequestsFreeUpAfterAMinute() {
        properties.setRequestsPerMinute(1);
        UUID user = UUID.randomUUID();

        assertThat(limiter.tryAcquire(user)).isTrue();
        assertThat(limiter.tryAcquire(user)).isFalse();

        clock.advanceSeconds(61);

        assertThat(limiter.tryAcquire(user)).isTrue();
    }

    @Test
    void eachUserHasTheirOwnAllowance() {
        properties.setRequestsPerMinute(1);

        assertThat(limiter.tryAcquire(UUID.randomUUID())).isTrue();
        assertThat(limiter.tryAcquire(UUID.randomUUID())).isTrue();
    }

    @Test
    void requireThrowsATooManyRequestsError() {
        properties.setRequestsPerMinute(1);
        UUID user = UUID.randomUUID();
        limiter.require(user);

        assertThatThrownBy(() -> limiter.require(user))
                .isInstanceOf(TooManyAiRequestsException.class)
                .satisfies(exception -> assertThat(((TooManyAiRequestsException) exception).status().value()).isEqualTo(429));
    }
}
