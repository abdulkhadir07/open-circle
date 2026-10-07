package com.opencircle.ai;

import com.opencircle.invitepost.LocalGlobalFeedQuery;
import com.opencircle.invitepost.FeedInvite;
import com.opencircle.user.AppUser;
import com.opencircle.user.ProfileInterests;
import com.opencircle.user.ProfileInterestsQuery;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

// A short "what's happening today" digest plus a one-line reason on invites that fit the person.
@Service
class FeedInsightsService {

    private static final int MAX_INVITES = 20;
    private static final int MAX_DIGEST_LENGTH = 220;
    private static final int MAX_REASON_LENGTH = 90;

    private final LocalGlobalFeedQuery feed;
    private final ProfileInterestsQuery profiles;
    private final AiClient aiClient;
    private final AiRateLimiter rateLimiter;
    private final AiJson json;
    private final TtlCache<String, FeedInsightsResponse> cache;

    FeedInsightsService(
            LocalGlobalFeedQuery feed,
            ProfileInterestsQuery profiles,
            AiClient aiClient,
            AiRateLimiter rateLimiter,
            AiJson json,
            AiProperties properties,
            Clock clock
    ) {
        this.feed = feed;
        this.profiles = profiles;
        this.aiClient = aiClient;
        this.rateLimiter = rateLimiter;
        this.json = json;
        this.cache = new TtlCache<>(clock, properties.getFeedInsightsCacheTtl());
    }

    FeedInsightsResponse insights(AppUser viewer) {
        List<FeedInvite> invites = feed.openInvites(viewer, MAX_INVITES).stream()
                .filter(invite -> !invite.posterId().equals(viewer.getId()))
                .toList();

        if (invites.isEmpty()) {
            return new FeedInsightsResponse("Quiet day so far. Be the first to start something.", List.of(), false);
        }

        ProfileInterests profile = profiles.forUser(viewer.getId());
        String cacheKey = viewer.getId() + ":" + fingerprint(invites, profile);

        FeedInsightsResponse cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        FeedInsightsResponse response = null;

        // Insights are passive, so being over the AI limit quietly means "use the simple version".
        if (aiClient.isEnabled() && rateLimiter.tryAcquire(viewer.getId())) {
            try {
                response = fromModel(viewer, invites, profile);
            } catch (AiUnavailableException exception) {
                // Fall back to rules below.
            }
        }

        if (response == null) {
            response = fallback(invites, profile);
        }

        cache.put(cacheKey, response);
        return response;
    }

    private FeedInsightsResponse fromModel(AppUser viewer, List<FeedInvite> invites, ProfileInterests profile) {
        JsonNode answer = json.parse(aiClient.generateJson(prompt(invites, profile), 0.6));

        String digest = AiJson.text(answer, "digest");
        if (digest == null) {
            throw new AiUnavailableException("No digest in model output");
        }
        if (digest.length() > MAX_DIGEST_LENGTH) {
            digest = digest.substring(0, MAX_DIGEST_LENGTH - 1) + "…";
        }

        Set<UUID> knownIds = new HashSet<>();
        invites.forEach(invite -> knownIds.add(invite.id()));

        List<FeedReason> reasons = new ArrayList<>();
        JsonNode items = answer.get("reasons");
        if (items != null && items.isArray()) {
            items.forEach(item -> {
                String id = AiJson.text(item, "id");
                String reason = AiJson.text(item, "reason");

                if (id == null || reason == null) {
                    return;
                }

                try {
                    UUID inviteId = UUID.fromString(id);
                    if (knownIds.contains(inviteId)) {
                        reasons.add(new FeedReason(inviteId, truncate(reason, MAX_REASON_LENGTH)));
                    }
                } catch (IllegalArgumentException ignored) {
                    // A made-up id from the model is simply dropped.
                }
            });
        }

        return new FeedInsightsResponse(digest, List.copyOf(reasons), true);
    }

    private String prompt(List<FeedInvite> invites, ProfileInterests profile) {
        StringBuilder list = new StringBuilder();
        for (FeedInvite invite : invites) {
            list.append("- id ").append(invite.id())
                    .append(": ").append(truncate(invite.content(), 160))
                    .append(" [topics: ").append(String.join(", ", invite.tags())).append("]\n");
        }

        return """
                You power the home feed of OpenCircle, an app where people post open invites to do things together.
                Tone: warm, brief, no emojis, no exclamation overload.
                Their interests: %s. Bio: %s.
                Open invites:
                %s
                Return JSON: {"digest": 1 or 2 sentences (max 200 characters) saying what is happening around them today and pointing out the best fit for this person,
                "reasons": [{"id": the invite id, "reason": one short sentence (max 80 characters) on why it fits them}] only for invites that really fit, using only the ids above}.
                """.formatted(
                profile.interests().isEmpty() ? "unknown" : String.join(", ", profile.interests()),
                profile.bio() == null ? "none" : truncate(profile.bio(), 200),
                list
        );
    }

    // Rules only: an invite "fits" when one of the person's interests shows up in its topics or text.
    static FeedInsightsResponse fallback(List<FeedInvite> invites, ProfileInterests profile) {
        List<FeedReason> reasons = new ArrayList<>();
        FeedInvite best = null;

        for (FeedInvite invite : invites) {
            String matched = matchingInterest(invite, profile.interests());

            if (matched != null) {
                reasons.add(new FeedReason(invite.id(), "Matches your interest in " + matched));
                if (best == null) {
                    best = invite;
                }
            }
        }

        String count = invites.size() + (invites.size() == 1 ? " open invite" : " open invites");
        String digest = best == null
                ? count + " around you today."
                : count + " around you today. \"" + truncate(best.content(), 60) + "\" looks like a good fit for you.";

        return new FeedInsightsResponse(digest, List.copyOf(reasons), false);
    }

    private static String matchingInterest(FeedInvite invite, List<String> interests) {
        String text = invite.content().toLowerCase(Locale.ROOT);

        for (String interest : interests) {
            String needle = interest.toLowerCase(Locale.ROOT);

            boolean inTopics = invite.tags().stream().anyMatch(tag -> tag.equals(needle) || tag.contains(needle) || needle.contains(tag));
            if (inTopics || text.contains(needle)) {
                return interest;
            }
        }

        return null;
    }

    private static String fingerprint(List<FeedInvite> invites, ProfileInterests profile) {
        return Objects.hash(
                invites.stream().map(invite -> invite.id() + "/" + invite.invitesLeft()).toList(),
                profile.interests(),
                profile.bio()
        ) + "";
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1).trim() + "…";
    }
}
