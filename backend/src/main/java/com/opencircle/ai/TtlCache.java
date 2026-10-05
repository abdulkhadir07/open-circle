package com.opencircle.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// A tiny time-limited cache for answers that are expensive to generate and fine to reuse briefly.
class TtlCache<K, V> {

    private record Entry<V>(V value, Instant expiresAt) {
    }

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    TtlCache(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    V get(K key) {
        Entry<V> entry = entries.get(key);

        if (entry == null) {
            return null;
        }

        if (!entry.expiresAt().isAfter(Instant.now(clock))) {
            entries.remove(key, entry);
            return null;
        }

        return entry.value();
    }

    void put(K key, V value) {
        if (entries.size() > 5_000) {
            Instant now = Instant.now(clock);
            entries.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
        }

        entries.put(key, new Entry<>(value, Instant.now(clock).plus(ttl)));
    }
}
