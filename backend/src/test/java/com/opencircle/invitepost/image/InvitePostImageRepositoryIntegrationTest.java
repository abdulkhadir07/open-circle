package com.opencircle.invitepost.image;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvitePostImageRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Autowired
    private UserService users;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private InvitePostImageRepository images;

    @Test
    void findsPostImagesInDisplayOrderAndCountsThem() {
        AppUser poster = users.createUser(
                "Image",
                "Poster",
                "image.repository@example.com",
                "hashed-password",
                "+14155559001",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        InvitePost post = posts.save(new InvitePost(
                poster,
                "Community picnic",
                InviteType.GROUP,
                4,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW
        ));

        InvitePostImage second = images.save(image(post, poster, "second.webp", "image/webp", "key-2", 2));
        InvitePostImage first = images.save(image(post, poster, "first.png", "image/png", "key-1", 1));
        images.flush();

        List<InvitePostImage> result = images.findByInvitePostIdInOrderByInvitePostIdAscDisplayOrderAsc(
                List.of(post.getId())
        );

        assertThat(result)
                .extracting(InvitePostImage::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(images.countByInvitePostId(post.getId())).isEqualTo(2L);
    }

    private InvitePostImage image(
            InvitePost post,
            AppUser poster,
            String filename,
            String contentType,
            String objectKey,
            int displayOrder
    ) {
        return new InvitePostImage(
                post,
                poster,
                filename,
                contentType,
                7L,
                "opencircle-test-images",
                "invite-post-images/" + objectKey,
                displayOrder,
                NOW.plusSeconds(displayOrder)
        );
    }
}
