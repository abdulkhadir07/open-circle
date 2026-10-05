package com.opencircle.ai;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// A per-user sliding window so one account can't run up the AI bill. In memory, which is fine for one node.
@Component
public class AiRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final AiProperties properties;
    private final Clock clock;
    private final Map<UUID, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();

    AiRateLimiter(AiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    // Records a request and returns true when the user is still under their limit.
    public boolean tryAcquire(UUID userId) {
        Instant now = Instant.now(clock);
        Deque<Instant> requests = requestsByUser.computeIfAbsent(userId, id -> new ArrayDeque<>());

        synchronized (requests) {
            while (!requests.isEmpty() && requests.peekFirst().isBefore(now.minus(WINDOW))) {
                requests.pollFirst();
            }

            if (requests.size() >= properties.getRequestsPerMinute()) {
                return false;
            }

            requests.addLast(now);
            return true;
        }
    }

    public void require(UUID userId) {
        if (!tryAcquire(userId)) {
            throw new TooManyAiRequestsException();
        }
    }
}
