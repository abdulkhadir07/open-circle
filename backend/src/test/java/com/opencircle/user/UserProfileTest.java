package com.opencircle.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileTest {

    private static final Instant CREATED_AT = Instant.parse("2026-09-12T12:00:00Z");

    @Test
    void newProfileUsesFirstNameAndStartsEmpty() {
        UserProfile profile = new UserProfile(user(), CREATED_AT);

        assertThat(profile.getDisplayName()).isEqualTo("Profile");
        assertThat(profile.getBio()).isNull();
        assertThat(profile.getInterests()).isEmpty();
        assertThat(profile.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(profile.getUpdatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void replacementNormalizesFieldsAndPreservesInterestOrder() {
        UserProfile profile = new UserProfile(user(), CREATED_AT);
        Instant updatedAt = CREATED_AT.plusSeconds(60);

        profile.replace(
                "  Curious Neighbor  ",
                "  Always up for learning something new.  ",
                List.of("  Hiking  ", "Coffee", "Live Music"),
                updatedAt
        );

        assertThat(profile.getDisplayName()).isEqualTo("Curious Neighbor");
        assertThat(profile.getBio()).isEqualTo("Always up for learning something new.");
        assertThat(profile.getInterests()).containsExactly("Hiking", "Coffee", "Live Music");
        assertThat(profile.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void blankBioAndEmptyInterestsClearOptionalFields() {
        UserProfile profile = new UserProfile(user(), CREATED_AT);
        profile.replace("Profile", "First bio", List.of("Hiking"), CREATED_AT.plusSeconds(30));

        profile.replace("Profile", "   ", List.of(), CREATED_AT.plusSeconds(60));

        assertThat(profile.getBio()).isNull();
        assertThat(profile.getInterests()).isEmpty();
    }

    @Test
    void duplicateInterestsAreRejectedWithoutPartiallyMutatingProfile() {
        UserProfile profile = new UserProfile(user(), CREATED_AT);
        profile.replace("Original", "Original bio", List.of("Coffee"), CREATED_AT.plusSeconds(30));

        assertThatThrownBy(() -> profile.replace(
                "Changed",
                "Changed bio",
                List.of("Hiking", "hIkInG"),
                CREATED_AT.plusSeconds(60)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Interests must be unique ignoring case");

        assertThat(profile.getDisplayName()).isEqualTo("Original");
        assertThat(profile.getBio()).isEqualTo("Original bio");
        assertThat(profile.getInterests()).containsExactly("Coffee");
        assertThat(profile.getUpdatedAt()).isEqualTo(CREATED_AT.plusSeconds(30));
    }

    @Test
    void moreThanEightInterestsAreRejected() {
        UserProfile profile = new UserProfile(user(), CREATED_AT);

        assertThatThrownBy(() -> profile.replace(
                "Profile",
                null,
                List.of("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"),
                CREATED_AT.plusSeconds(60)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A profile can have at most 8 interests");
    }

    private AppUser user() {
        return new AppUser(
                "profile_user_1234",
                "Profile",
                "User",
                "profile.user@example.com",
                "hashed-password",
                "+14155559001",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
