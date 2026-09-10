package com.opencircle.profileimage;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProfileImageRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

    @Autowired
    private UserService users;

    @Autowired
    private ProfileImageRepository images;

    @Test
    void findsProfileImagesForDistinctUserIdsInOneBatch() {
        AppUser firstUser = user("first.profile.repository@example.com", "+14155559011");
        AppUser secondUser = user("second.profile.repository@example.com", "+14155559012");
        ProfileImage firstImage = images.save(image(firstUser, "first.png", "first-key"));
        ProfileImage secondImage = images.save(image(secondUser, "second.webp", "second-key"));
        images.flush();

        assertThat(images.findAllByUser_IdIn(Set.of(firstUser.getId(), secondUser.getId())))
                .extracting(ProfileImage::getId)
                .containsExactlyInAnyOrder(firstImage.getId(), secondImage.getId());
        assertThat(images.findByUser_Id(firstUser.getId()))
                .contains(firstImage);
    }

    private ProfileImage image(AppUser user, String filename, String objectKey) {
        return new ProfileImage(
                user,
                filename,
                filename.endsWith("webp") ? "image/webp" : "image/png",
                7L,
                "opencircle-test-images",
                "profile-images/users/" + user.getId() + "/" + objectKey,
                NOW
        );
    }

    private AppUser user(String email, String phoneNumber) {
        return users.createUser(
                "Profile",
                "User",
                email,
                "hashed-password",
                phoneNumber,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
