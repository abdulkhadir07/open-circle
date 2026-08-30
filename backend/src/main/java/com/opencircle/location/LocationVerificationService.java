package com.opencircle.location;

import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
class LocationVerificationService {

    private final LocationResolver locationResolver;
    private final Clock clock;

    LocationVerificationService(
            LocationResolver locationResolver,
            Clock clock
    ) {
        this.locationResolver = locationResolver;
        this.clock = clock;
    }

    @Transactional
    AppUser verifyLocation(AppUser user, VerifyLocationRequest request) {
        // Resolves trusted city/state/country from device coordinates instead of user-entered text.
        ResolvedLocation resolvedLocation = locationResolver.resolve(
                request.latitude(),
                request.longitude()
        );

        user.verifyLocation(
                resolvedLocation.city(),
                resolvedLocation.stateRegion(),
                resolvedLocation.country(),
                Instant.now(clock)
        );

        return user;
    }
}