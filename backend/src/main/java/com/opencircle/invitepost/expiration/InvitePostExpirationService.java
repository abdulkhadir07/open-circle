package com.opencircle.invitepost.expiration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
class InvitePostExpirationService {

    private static final Logger log = LoggerFactory.getLogger(InvitePostExpirationService.class);

    private final InvitePostExpirationRepository expirations;
    private final InvitePostExpirationProcessor processor;
    private final Clock clock;

    InvitePostExpirationService(
            InvitePostExpirationRepository expirations,
            InvitePostExpirationProcessor processor,
            Clock clock
    ) {
        this.expirations = expirations;
        this.processor = processor;
        this.clock = clock;
    }

    int processDuePosts() {
        Instant now = Instant.now(clock);
        int processed = 0;

        for (InvitePostExpirationCandidate candidate : expirations.findDue(now)) {
            try {
                if (processor.process(candidate, now)) {
                    processed++;
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Failed to process expiration notifications for invite post {}",
                        candidate.postId(),
                        exception
                );
            }
        }

        return processed;
    }
}
