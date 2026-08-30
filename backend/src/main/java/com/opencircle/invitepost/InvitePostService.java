package com.opencircle.invitepost;

import com.opencircle.location.LocationNotVerifiedException;
import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
class InvitePostService {

    private final InvitePostRepository posts;
    private final Clock clock;

    InvitePostService(InvitePostRepository posts, Clock clock) {
        this.posts = posts;
        this.clock = clock;
    }

    // Creates an invite post using the poster's verified location snapshot.
    @Transactional
    InvitePost createPost(AppUser poster, CreateInvitePostRequest request) {
        requireVerifiedLocation(poster);

        int totalCapacity = totalCapacityFor(request);
        validateLocationScope(poster, request.locationScope());

        InvitePost post = new InvitePost(
                poster,
                request.content(),
                request.inviteType(),
                totalCapacity,
                request.locationScope(),
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                Instant.now(clock)
        );

        return posts.save(post);
    }

    // Returns local posts that match the viewer's verified location.
    @Transactional(readOnly = true)
    List<InvitePost> getLocalFeed(AppUser viewer, LocationScope scope) {
        requireVerifiedLocation(viewer);

        Instant now = Instant.now(clock);

        if (scope == null) {
            return defaultLocalFeed(viewer, now);
        }

        return switch (scope) {
            case COUNTRY -> countryFeed(viewer, now);
            case STATE_REGION -> stateRegionFeed(viewer, now);
            case CITY -> cityFeed(viewer, now);
            case GLOBAL -> throw new InvalidInvitePostRequestException("Global scope is not part of the local feed");
        };
    }

    // Returns globally visible invite posts after confirming the viewer has verified location.
    @Transactional(readOnly = true)
    List<InvitePost> getGlobalFeed(AppUser viewer) {
        requireVerifiedLocation(viewer);

        return posts.findGlobalFeed(
                InvitePostStatus.ACTIVE,
                Instant.now(clock),
                LocationScope.GLOBAL
        );
    }

    private void requireVerifiedLocation(AppUser user) {
        if (!user.hasVerifiedLocation()) {
            throw new LocationNotVerifiedException();
        }
    }

    private int totalCapacityFor(CreateInvitePostRequest request) {
        if (request.inviteType() == InviteType.SINGLE) {
            if (request.totalCapacity() != null && request.totalCapacity() != 1) {
                throw new InvalidInvitePostRequestException("Single invites must have a total capacity of 1");
            }

            return 1;
        }

        if (request.totalCapacity() == null || request.totalCapacity() < 2) {
            throw new InvalidInvitePostRequestException("Group invites must have a total capacity of at least 2");
        }

        return request.totalCapacity();
    }

    private void validateLocationScope(AppUser user, LocationScope scope) {
        if (scope == LocationScope.STATE_REGION && !hasText(user.getVerifiedStateRegion())) {
            throw new InvalidInvitePostRequestException("State/region scope is not available for your verified location");
        }
    }

    private List<InvitePost> defaultLocalFeed(AppUser viewer, Instant now) {
        List<InvitePost> feed = new ArrayList<>();

        feed.addAll(countryFeed(viewer, now));

        if (hasText(viewer.getVerifiedStateRegion())) {
            feed.addAll(stateRegionFeed(viewer, now));
        }

        feed.addAll(cityFeed(viewer, now));

        return newestFirst(feed);
    }

    private List<InvitePost> countryFeed(AppUser viewer, Instant now) {
        return posts.findCountryFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.COUNTRY,
                viewer.getVerifiedCountry()
        );
    }

    private List<InvitePost> stateRegionFeed(AppUser viewer, Instant now) {
        validateLocationScope(viewer, LocationScope.STATE_REGION);

        return posts.findStateRegionFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.STATE_REGION,
                viewer.getVerifiedCountry(),
                viewer.getVerifiedStateRegion()
        );
    }

    private List<InvitePost> cityFeed(AppUser viewer, Instant now) {
        return posts.findCityFeed(
                InvitePostStatus.ACTIVE,
                now,
                LocationScope.CITY,
                viewer.getVerifiedCountry(),
                viewer.getVerifiedCity()
        );
    }

    private List<InvitePost> newestFirst(List<InvitePost> feed) {
        return feed.stream()
                .sorted(Comparator.comparing(InvitePost::getCreatedAt).reversed())
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}