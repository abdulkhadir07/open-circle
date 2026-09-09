package com.opencircle.invitepost.image;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitePostImageTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Test
    void constructorCreatesOrderedImageMetadataForPoster() {
        AppUser poster = user("poster.image@example.com");
        InvitePost post = post(poster);

        InvitePostImage image = new InvitePostImage(
                post,
                poster,
                " flyer.png ",
                " image/png ",
                7L,
                " image-bucket ",
                " invite-post-images/post-id/file-id ",
                2,
                NOW
        );

        assertThat(image.getInvitePost()).isSameAs(post);
        assertThat(image.getUploader()).isSameAs(poster);
        assertThat(image.getOriginalFilename()).isEqualTo("flyer.png");
        assertThat(image.getContentType()).isEqualTo("image/png");
        assertThat(image.getFileSizeBytes()).isEqualTo(7L);
        assertThat(image.getS3Bucket()).isEqualTo("image-bucket");
        assertThat(image.getS3ObjectKey()).isEqualTo("invite-post-images/post-id/file-id");
        assertThat(image.getDisplayOrder()).isEqualTo(2);
        assertThat(image.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void constructorRejectsUploaderWhoIsNotPoster() {
        AppUser poster = user("poster.forbidden@example.com");

        assertThatThrownBy(() -> new InvitePostImage(
                post(poster),
                user("outsider.forbidden@example.com"),
                "flyer.png",
                "image/png",
                7L,
                "bucket",
                "key",
                1,
                NOW
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the invite post poster can upload images");
    }

    @Test
    void constructorRejectsInvalidDisplayOrder() {
        AppUser poster = user("poster.order@example.com");

        assertThatThrownBy(() -> new InvitePostImage(
                post(poster),
                poster,
                "flyer.png",
                "image/png",
                7L,
                "bucket",
                "key",
                5,
                NOW
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Display order must be between 1 and 4");
    }

    private InvitePost post(AppUser poster) {
        return new InvitePost(
                poster,
                "Community picnic",
                InviteType.GROUP,
                4,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW
        );
    }

    private AppUser user(String email) {
        return new AppUser(
                "test_" + Math.abs(email.hashCode()),
                "Test",
                "User",
                email,
                "hashed-password",
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
