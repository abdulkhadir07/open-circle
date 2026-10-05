package com.opencircle.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// A cheap public read of just a person's bio and interests (the full profile also loads ratings and awards).
@Service
public class ProfileInterestsQuery {

    private final UserProfileRepository profiles;

    ProfileInterestsQuery(UserProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public ProfileInterests forUser(UUID userId) {
        return profiles.findForDisplay(userId)
                .map(profile -> new ProfileInterests(profile.getBio(), profile.getInterests()))
                .orElse(ProfileInterests.EMPTY);
    }
}
