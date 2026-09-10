package com.opencircle.profileimage;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageTest {

    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

    @Test
    void constructorCreatesNormalizedImageMetadata() {
        AppUser user = user();

        ProfileImage image = new ProfileImage(
                user,
                " avatar.png ",
                " image/png ",
                7L,
                " profile-bucket ",
                " profile-images/users/user-id/file-id ",
                NOW
        );

        assertThat(image.getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(image.getContentType()).isEqualTo("image/png");
        assertThat(image.getFileSizeBytes()).isEqualTo(7L);
        assertThat(image.getS3Bucket()).isEqualTo("profile-bucket");
        assertThat(image.getS3ObjectKey()).isEqualTo("profile-images/users/user-id/file-id");
        assertThat(image.getCreatedAt()).isEqualTo(NOW);
        assertThat(image.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void replacePreservesCreationTimeAndUpdatesStorageMetadata() {
        ProfileImage image = image();
        Instant replacedAt = NOW.plusSeconds(60);

        image.replace(
                "new.webp",
                "image/webp",
                12L,
                "new-bucket",
                "profile-images/users/user-id/new-file-id",
                replacedAt
        );

        assertThat(image.getOriginalFilename()).isEqualTo("new.webp");
        assertThat(image.getContentType()).isEqualTo("image/webp");
        assertThat(image.getFileSizeBytes()).isEqualTo(12L);
        assertThat(image.getS3Bucket()).isEqualTo("new-bucket");
        assertThat(image.getS3ObjectKey()).endsWith("new-file-id");
        assertThat(image.getCreatedAt()).isEqualTo(NOW);
        assertThat(image.getUpdatedAt()).isEqualTo(replacedAt);
    }

    @Test
    void constructorRejectsEmptyFile() {
        assertThatThrownBy(() -> new ProfileImage(
                user(),
                "avatar.png",
                "image/png",
                0L,
                "bucket",
                "key",
                NOW
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File size must be greater than zero");
    }

    private ProfileImage image() {
        return new ProfileImage(
                user(),
                "avatar.png",
                "image/png",
                7L,
                "profile-bucket",
                "profile-images/users/user-id/file-id",
                NOW
        );
    }

    private AppUser user() {
        return new AppUser(
                "profile_user",
                "Profile",
                "User",
                "profile.image@example.com",
                "hashed-password",
                "+14155559010",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
