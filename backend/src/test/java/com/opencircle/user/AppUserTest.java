package com.opencircle.user;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    private AppUser newUser() {
        return new AppUser(
                "  Swift_Falcon_1234 ",
                "Jane",
                "Doe",
                "  Test@Example.COM ",
                "hashed-pw",
                "+12025550100",
                LocalDate.of(1995, 1, 1),
                "San Francisco",
                "California",
                "United States"
        );
    }

    @Test
    void constructorNormalizesUsernameAndEmail() {
        AppUser user = newUser();

        assertThat(user.getUsername()).isEqualTo("swift_falcon_1234");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void onCreateSetsCreatedAndUpdatedTimestamps() {
        AppUser user = newUser();

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isEqualTo(user.getCreatedAt());
    }

    @Test
    void onUpdateChangesOnlyUpdatedAt() throws InterruptedException {
        AppUser user = newUser();
        user.onCreate();
        var originalCreatedAt = user.getCreatedAt();

        Thread.sleep(10);
        user.onUpdate();

        assertThat(user.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(user.getUpdatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    void markEmailVerifiedUpdatesVerificationState() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        Instant verifiedAt = Instant.now();

        user.markEmailVerified(verifiedAt);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void changePasswordUpdatesPasswordHash() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "old-hash",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        user.changePassword("new-hash");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void verifyLocationStoresVerifiedLocationState() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                null,
                "USA"
        );

        Instant verifiedAt = Instant.parse("2026-08-23T12:00:00Z");

        user.verifyLocation(
                "Banjul",
                null,
                "The Gambia",
                verifiedAt
        );

        assertThat(user.getVerifiedCity()).isEqualTo("Banjul");
        assertThat(user.getVerifiedStateRegion()).isNull();
        assertThat(user.getVerifiedCountry()).isEqualTo("The Gambia");
        assertThat(user.getLocationVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(user.getLocationSource()).isEqualTo(LocationSource.DEVICE);
    }

    @Test
    void hasVerifiedLocationReturnsFalseBeforeLocationIsVerified() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                null,
                "USA"
        );

        assertThat(user.hasVerifiedLocation()).isFalse();
    }

    @Test
    void hasVerifiedLocationReturnsTrueAfterLocationIsVerified() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                null,
                "USA"
        );

        user.verifyLocation(
                "Banjul",
                null,
                "The Gambia",
                Instant.parse("2026-08-29T12:00:00Z")
        );

        assertThat(user.hasVerifiedLocation()).isTrue();
    }
}
