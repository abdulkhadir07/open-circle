package com.opencircle.user;

import org.junit.jupiter.api.Test;

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
}
