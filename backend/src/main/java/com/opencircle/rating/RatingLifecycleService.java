package com.opencircle.rating;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RatingLifecycleService {

    private static final Duration INACTIVITY_DELAY = Duration.ofDays(3);
    private static final Duration MAX_INTERACTION_DURATION = Duration.ofDays(14);

    private final RatingLifecycleQueryRepository lifecycleQueries;
    private final RatingObligationRepository obligations;
    private final RatingRepository ratings;
    private final Clock clock;

    RatingLifecycleService(
            RatingLifecycleQueryRepository lifecycleQueries,
            RatingObligationRepository obligations,
            RatingRepository ratings,
            Clock clock
    ) {
        this.lifecycleQueries = lifecycleQueries;
        this.obligations = obligations;
        this.ratings = ratings;
        this.clock = clock;
    }

    @Transactional
    void reconcileEngagement(UUID engagementId, Instant now) {
        lifecycleQueries.findMonitoringForEngagement(engagementId)
                .forEach(snapshot -> activateIfReady(snapshot, now, null));
    }

    @Transactional
    public void reconcileInvitePost(UUID invitePostId, Instant now) {
        lifecycleQueries.findMonitoringForInvitePost(invitePostId)
                .forEach(snapshot -> activateIfReady(snapshot, now, null));
    }

    @Transactional
    public void handleParticipantExit(UUID invitePostId, UUID exitingUserId, Instant exitedAt) {
        lifecycleQueries.findMonitoringForExit(invitePostId, exitingUserId).forEach(snapshot -> {
            if (snapshot.isQualified()) {
                activateIfReady(snapshot, exitedAt, exitedAt);
            } else {
                markPairNotRequired(snapshot.engagementId(), exitedAt);
            }
        });
    }

    @Transactional
    void reconcileUser(UUID userId) {
        Instant now = Instant.now(clock);
        lifecycleQueries.findReadyForUser(userId, now)
                .forEach(snapshot -> activateIfReady(snapshot, now, null));
        obligations.markExpiredAsMissedForParticipant(userId, now);
        ratings.revealResolvedRatings(now);
    }

    @Transactional
    void finalizeEngagement(UUID engagementId, Instant now) {
        obligations.markExpiredAsMissedForEngagement(engagementId, now);
        ratings.revealResolvedRatings(now);
    }

    @Transactional
    RatingLifecycleResult runLifecycle() {
        Instant now = Instant.now(clock);
        int activated = 0;

        for (RatingLifecycleSnapshot snapshot : lifecycleQueries.findReady(now)) {
            if (activateIfReady(snapshot, now, null)) {
                activated++;
            }
        }

        int missed = obligations.markAllExpiredAsMissed(now);
        int revealed = ratings.revealResolvedRatings(now);

        return new RatingLifecycleResult(activated, missed, revealed);
    }

    private boolean activateIfReady(RatingLifecycleSnapshot snapshot, Instant now, Instant participantExitAt) {
        if (!snapshot.isQualified()) {
            return false;
        }

        Requirement requirement = earliestRequirement(snapshot, now, participantExitAt);
        if (requirement == null) {
            return false;
        }

        List<RatingObligation> pair = obligations.findPairForUpdate(snapshot.engagementId());
        if (pair.size() != 2 || pair.stream().anyMatch(this::isNotMonitoring)) {
            return false;
        }

        pair.forEach(obligation -> obligation.require(requirement.trigger(), requirement.requiredAt()));
        obligations.saveAll(pair);
        return true;
    }

    private void markPairNotRequired(UUID engagementId, Instant decidedAt) {
        List<RatingObligation> pair = obligations.findPairForUpdate(engagementId);
        if (pair.size() != 2 || pair.stream().anyMatch(this::isNotMonitoring)) {
            return;
        }

        pair.forEach(obligation -> obligation.markNotRequired(decidedAt));
        obligations.saveAll(pair);
    }

    private Requirement earliestRequirement(
            RatingLifecycleSnapshot snapshot,
            Instant now,
            Instant participantExitAt
    ) {
        List<Requirement> applicable = new ArrayList<>();
        addIfReached(
                applicable,
                RatingTrigger.CHAT_INACTIVITY,
                snapshot.lastRoomActivityAt().plus(INACTIVITY_DELAY),
                now
        );
        addIfReached(
                applicable,
                RatingTrigger.MAX_DURATION,
                snapshot.acceptedAt().plus(MAX_INTERACTION_DURATION),
                now
        );

        if (participantExitAt != null) {
            applicable.add(new Requirement(RatingTrigger.PARTICIPANT_EXIT, participantExitAt));
        }

        return applicable.stream()
                .min(Comparator.comparing(Requirement::requiredAt).thenComparing(Requirement::trigger))
                .orElse(null);
    }

    private void addIfReached(
            List<Requirement> applicable,
            RatingTrigger trigger,
            Instant requiredAt,
            Instant now
    ) {
        if (!requiredAt.isAfter(now)) {
            applicable.add(new Requirement(trigger, requiredAt));
        }
    }

    private boolean isNotMonitoring(RatingObligation obligation) {
        return obligation.getStatus() != RatingObligationStatus.MONITORING;
    }

    private record Requirement(RatingTrigger trigger, Instant requiredAt) {
    }
}
