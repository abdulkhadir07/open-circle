package com.opencircle.profileimage;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.security.JwtService;
import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileImageControllerIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(1000);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private ProfileImageRepository images;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StorageService storageService;

    @Test
    void currentUserCanUploadProfileImageAndReceiveViewUrl() throws Exception {
        AppUser user = user("upload");
        String key = objectKey(user, "stored-file");
        Instant urlExpiresAt = Instant.parse("2026-09-09T13:00:00Z");

        when(storageService.upload(
                eq("profile-images/users/" + user.getId()),
                any(),
                eq(7L),
                eq("image/png")
        )).thenReturn(new StoredFile("profile-bucket", key, "image/png", 7L));
        when(storageService.generateViewUrl("profile-bucket", key))
                .thenReturn(new StorageAccessUrl(URI.create("https://example.com/profile.png"), urlExpiresAt));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/profile-image")
                        .file(imageFile("avatar.png", "image/png", "content".getBytes()))
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.url").value("https://example.com/profile.png"))
                .andExpect(jsonPath("$.urlExpiresAt").value(urlExpiresAt.toString()))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.originalFilename").doesNotExist())
                .andExpect(jsonPath("$.fileSizeBytes").doesNotExist())
                .andExpect(jsonPath("$.s3Bucket").doesNotExist())
                .andExpect(jsonPath("$.s3ObjectKey").doesNotExist());

        ProfileImage savedImage = images.findByUser_Id(user.getId()).orElseThrow();
        assertThat(savedImage.getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(savedImage.getContentType()).isEqualTo("image/png");
        assertThat(savedImage.getFileSizeBytes()).isEqualTo(7L);
        assertThat(savedImage.getS3Bucket()).isEqualTo("profile-bucket");
        assertThat(savedImage.getS3ObjectKey()).isEqualTo(key);
        verify(storageService, never()).delete(any(), any());
    }

    @Test
    void uploadReplacesExistingImageAndDeletesOldObjectAfterCommit() throws Exception {
        AppUser user = user("replace");
        ProfileImage existingImage = images.saveAndFlush(storedImage(user, "old-file", "image/png"));
        UUID imageId = existingImage.getId();
        String oldKey = existingImage.getS3ObjectKey();
        String newKey = objectKey(user, "new-file");

        when(storageService.upload(
                eq("profile-images/users/" + user.getId()),
                any(),
                eq(11L),
                eq("image/webp")
        )).thenReturn(new StoredFile("profile-bucket", newKey, "image/webp", 11L));
        when(storageService.generateViewUrl("profile-bucket", newKey))
                .thenReturn(new StorageAccessUrl(
                        URI.create("https://example.com/new-profile.webp"),
                        Instant.parse("2026-09-09T14:00:00Z")
                ));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/profile-image")
                        .file(imageFile("new-profile.webp", "image/webp", "new-content".getBytes()))
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(imageId.toString()))
                .andExpect(jsonPath("$.url").value("https://example.com/new-profile.webp"))
                .andExpect(jsonPath("$.contentType").value("image/webp"));

        ProfileImage replacedImage = images.findByUser_Id(user.getId()).orElseThrow();
        assertThat(replacedImage.getId()).isEqualTo(imageId);
        assertThat(replacedImage.getOriginalFilename()).isEqualTo("new-profile.webp");
        assertThat(replacedImage.getS3ObjectKey()).isEqualTo(newKey);
        verify(storageService).delete("profile-bucket", oldKey);
        verify(storageService, never()).delete("profile-bucket", newKey);
    }

    @Test
    void currentUserCanDeleteProfileImage() throws Exception {
        AppUser user = user("delete");
        ProfileImage image = images.saveAndFlush(storedImage(user, "delete-file", "image/jpeg"));

        mockMvc.perform(delete("/api/users/me/profile-image")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());

        assertThat(images.findByUser_Id(user.getId())).isEmpty();
        verify(storageService).delete(image.getS3Bucket(), image.getS3ObjectKey());
    }

    @Test
    void deletingMissingProfileImageIsIdempotent() throws Exception {
        AppUser user = user("delete-missing");

        mockMvc.perform(delete("/api/users/me/profile-image")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isNoContent());

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsUnsupportedImageType() throws Exception {
        AppUser user = user("invalid-type");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/profile-image")
                        .file(imageFile("avatar.gif", "image/gif", "content".getBytes()))
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File type is not supported"));

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadRejectsImageLargerThanFiveMegabytes() throws Exception {
        AppUser user = user("invalid-size");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/profile-image")
                        .file(imageFile("avatar.jpg", "image/jpeg", new byte[5_242_881]))
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("File size exceeds the maximum allowed profile image size"));

        verifyNoInteractions(storageService);
    }

    @Test
    void meReturnsProfileImageWhenPresent() throws Exception {
        AppUser user = user("me-image");
        ProfileImage savedImage = images.saveAndFlush(storedImage(user, "me-file", "image/png"));
        ProfileImage image = images.findById(savedImage.getId()).orElseThrow();
        Instant urlExpiresAt = Instant.parse("2026-09-09T15:00:00Z");

        when(storageService.generateViewUrl(image.getS3Bucket(), image.getS3ObjectKey()))
                .thenReturn(new StorageAccessUrl(URI.create("https://example.com/me.png"), urlExpiresAt));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.profileImage.id").value(image.getId().toString()))
                .andExpect(jsonPath("$.profileImage.url").value("https://example.com/me.png"))
                .andExpect(jsonPath("$.profileImage.urlExpiresAt").value(urlExpiresAt.toString()))
                .andExpect(jsonPath("$.profileImage.contentType").value("image/png"))
                .andExpect(jsonPath("$.profileImage.updatedAt").value(image.getUpdatedAt().toString()));
    }

    @Test
    void meReturnsNullProfileImageWhenMissing() throws Exception {
        AppUser user = user("me-missing");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImage").isEmpty());

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAndDeleteRequireAuthentication() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/users/me/profile-image")
                        .file(imageFile("avatar.png", "image/png", "content".getBytes())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/users/me/profile-image"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(storageService);
    }

    private ProfileImage storedImage(AppUser user, String fileId, String contentType) {
        return new ProfileImage(
                user,
                "avatar." + contentType.substring(contentType.indexOf('/') + 1),
                contentType,
                7L,
                "profile-bucket",
                objectKey(user, fileId),
                Instant.now()
        );
    }

    private MockMultipartFile imageFile(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }

    private String objectKey(AppUser user, String fileId) {
        return "profile-images/users/" + user.getId() + "/" + fileId;
    }

    private String bearerToken(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private AppUser user(String label) {
        int sequence = USER_SEQUENCE.incrementAndGet();

        return users.createUser(
                "Profile",
                "User",
                label + ".profile." + sequence + "@example.com",
                "hashed-password",
                "+1415555" + sequence,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
