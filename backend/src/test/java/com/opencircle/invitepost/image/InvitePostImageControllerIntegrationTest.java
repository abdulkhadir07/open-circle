package com.opencircle.invitepost.image;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvitePostImageControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private InvitePostImageRepository images;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StorageService storageService;

    @Test
    void posterCanUploadImageAndReceiveViewUrl() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.upload@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "Community picnic", LocationScope.CITY, now.minusSeconds(60));
        String objectKey = "invite-post-images/invite-posts/" + post.getId() + "/stored-file";
        Instant urlExpiresAt = now.plusSeconds(3600);

        when(storageService.upload(
                eq("invite-post-images/invite-posts/" + post.getId()),
                any(),
                eq(7L),
                eq("image/png")
        )).thenReturn(new StoredFile("opencircle-test-images", objectKey, "image/png", 7L));
        when(storageService.generateViewUrl("opencircle-test-images", objectKey, post.getExpiresAt()))
                .thenReturn(new StorageAccessUrl(URI.create("https://example.com/view/flyer.png"), urlExpiresAt));

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("flyer.png", "image/png", "content".getBytes()))
                        .header("Authorization", "Bearer " + loginToken(poster.getEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url").value("https://example.com/view/flyer.png"))
                .andExpect(jsonPath("$.urlExpiresAt").value(urlExpiresAt.toString()))
                .andExpect(jsonPath("$.originalFilename").value("flyer.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.fileSizeBytes").value(7))
                .andExpect(jsonPath("$.displayOrder").value(1))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.s3Bucket").doesNotExist())
                .andExpect(jsonPath("$.s3ObjectKey").doesNotExist());

        List<InvitePostImage> savedImages = images.findByInvitePostIdInOrderByInvitePostIdAscDisplayOrderAsc(
                List.of(post.getId())
        );
        assertThat(savedImages).singleElement().satisfies(image -> {
            assertThat(image.getUploader().getId()).isEqualTo(poster.getId());
            assertThat(image.getS3Bucket()).isEqualTo("opencircle-test-images");
            assertThat(image.getS3ObjectKey()).isEqualTo(objectKey);
            assertThat(image.getDisplayOrder()).isEqualTo(1);
        });
        verify(storageService).generateViewUrl("opencircle-test-images", objectKey, post.getExpiresAt());
    }

    @Test
    void nonPosterCannotUploadImage() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.forbidden@example.com", "San Francisco", "California", "USA");
        AppUser outsider = verifiedUser("outsider.image.forbidden@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "Poster only", LocationScope.CITY, now.minusSeconds(60));

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("flyer.png", "image/png", "content".getBytes()))
                        .header("Authorization", "Bearer " + loginToken(outsider.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the invite post poster can upload images"));

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsUnsupportedImageType() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.type@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "No GIFs", LocationScope.CITY, now.minusSeconds(60));

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("animation.gif", "image/gif", "content".getBytes()))
                        .header("Authorization", "Bearer " + loginToken(poster.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File type is not supported"));

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsImageLargerThanFiveMegabytes() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.size@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "Small images", LocationScope.CITY, now.minusSeconds(60));

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("large.webp", "image/webp", new byte[5_242_881]))
                        .header("Authorization", "Bearer " + loginToken(poster.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("File size exceeds the maximum allowed invite post image size"));

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsFifthImage() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.limit@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "Four images only", LocationScope.CITY, now.minusSeconds(60));

        for (int displayOrder = 1; displayOrder <= 4; displayOrder++) {
            images.save(storedImage(post, poster, displayOrder, now));
        }
        images.flush();

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("fifth.jpg", "image/jpeg", "content".getBytes()))
                        .header("Authorization", "Bearer " + loginToken(poster.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An invite post can have at most 4 images"));

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsExpiredPost() throws Exception {
        Instant now = Instant.now();
        AppUser poster = verifiedUser("poster.image.expired@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(
                poster,
                "Already expired",
                LocationScope.CITY,
                now.minusSeconds(25 * 60L * 60L)
        );

        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", post.getId())
                        .file(imageFile("late.png", "image/png", "content".getBytes()))
                        .header("Authorization", "Bearer " + loginToken(poster.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Images cannot be uploaded to an expired invite post"));

        verifyNoInteractions(storageService);
    }

    @Test
    void visibleFeedIncludesOrderedImageViewUrls() throws Exception {
        Instant now = Instant.now();
        verifiedUser("viewer.image.feed@example.com", "San Francisco", "California", "USA");
        AppUser poster = verifiedUser("poster.image.feed@example.com", "San Francisco", "California", "USA");
        InvitePost post = savedPost(poster, "Image feed post", LocationScope.CITY, now.minusSeconds(60));
        InvitePostImage image = images.saveAndFlush(new InvitePostImage(
                post,
                poster,
                "picnic.jpg",
                "image/jpeg",
                12L,
                "opencircle-test-images",
                "invite-post-images/feed-image",
                1,
                now.minusSeconds(30)
        ));
        Instant urlExpiresAt = now.plusSeconds(3600);

        when(storageService.generateViewUrl(
                image.getS3Bucket(),
                image.getS3ObjectKey(),
                post.getExpiresAt()
        )).thenReturn(new StorageAccessUrl(
                URI.create("https://example.com/view/picnic.jpg"),
                urlExpiresAt
        ));

        mockMvc.perform(get("/api/invite-posts/local")
                        .header("Authorization", "Bearer " + loginToken("viewer.image.feed@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].content").value("Image feed post"))
                .andExpect(jsonPath("$[0].images", hasSize(1)))
                .andExpect(jsonPath("$[0].images[0].id").value(image.getId().toString()))
                .andExpect(jsonPath("$[0].images[0].url").value("https://example.com/view/picnic.jpg"))
                .andExpect(jsonPath("$[0].images[0].urlExpiresAt").value(urlExpiresAt.toString()))
                .andExpect(jsonPath("$[0].images[0].originalFilename").value("picnic.jpg"))
                .andExpect(jsonPath("$[0].images[0].displayOrder").value(1));

        verify(storageService).generateViewUrl(
                image.getS3Bucket(),
                image.getS3ObjectKey(),
                post.getExpiresAt()
        );
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/invite-posts/{postId}/images", UUID.randomUUID())
                        .file(imageFile("flyer.png", "image/png", "content".getBytes())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(storageService);
    }

    private MockMultipartFile imageFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private InvitePost savedPost(
            AppUser poster,
            String content,
            LocationScope scope,
            Instant createdAt
    ) {
        return posts.saveAndFlush(new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                4,
                scope,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                createdAt
        ));
    }

    private InvitePostImage storedImage(
            InvitePost post,
            AppUser poster,
            int displayOrder,
            Instant createdAt
    ) {
        return new InvitePostImage(
                post,
                poster,
                "image-" + displayOrder + ".png",
                "image/png",
                7L,
                "opencircle-test-images",
                "invite-post-images/limit-image-" + displayOrder,
                displayOrder,
                createdAt.plusSeconds(displayOrder)
        );
    }

    private AppUser verifiedUser(
            String email,
            String city,
            String stateRegion,
            String country
    ) {
        AppUser user = users.createUser(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                city,
                stateRegion,
                country
        );
        Instant verifiedAt = Instant.now();
        user.markEmailVerified(verifiedAt);
        user.verifyLocation(city, stateRegion, country, verifiedAt);
        return user;
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }
}
