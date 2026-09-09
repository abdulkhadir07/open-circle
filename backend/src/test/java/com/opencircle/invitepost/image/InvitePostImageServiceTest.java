package com.opencircle.invitepost.image;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InvitePostStatus;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InvitePostImageServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private static final UUID POST_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final InvitePostRepository posts = mock(InvitePostRepository.class);
    private final InvitePostImageRepository images = mock(InvitePostImageRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final StorageProperties storageProperties = storageProperties();
    private final InvitePostImageProperties imageProperties = imageProperties();
    private final InvitePostImageService service = new InvitePostImageService(
            posts,
            images,
            storageService,
            storageProperties,
            imageProperties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void uploadImageStoresMetadataAndReturnsViewUrl() {
        AppUser poster = user("poster.upload.image@example.com");
        InvitePost post = post(poster, NOW.minusSeconds(60));
        StorageAccessUrl accessUrl = new StorageAccessUrl(
                URI.create("https://example.com/view/flyer.png"),
                NOW.plusSeconds(3600)
        );

        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.of(post));
        when(images.countByInvitePostId(POST_ID)).thenReturn(1L);
        when(storageService.upload(
                eq("invite-post-images/invite-posts/" + POST_ID),
                any(),
                eq(7L),
                eq("image/png")
        )).thenReturn(new StoredFile("bucket", "key", "image/png", 7L));
        when(images.save(any(InvitePostImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.generateViewUrl("bucket", "key", post.getExpiresAt())).thenReturn(accessUrl);

        InvitePostImageResponse response = service.uploadImage(
                poster,
                POST_ID,
                upload("flyer.png", " IMAGE/PNG ", 7L)
        );

        assertThat(response.url()).isEqualTo("https://example.com/view/flyer.png");
        assertThat(response.urlExpiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(response.originalFilename()).isEqualTo("flyer.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.fileSizeBytes()).isEqualTo(7L);
        assertThat(response.displayOrder()).isEqualTo(2);
        assertThat(response.createdAt()).isEqualTo(NOW);

        verify(storageService).generateViewUrl("bucket", "key", post.getExpiresAt());
    }

    @Test
    void uploadImageRejectsUnsupportedTypeBeforeLookingUpPost() {
        assertThatThrownBy(() -> service.uploadImage(
                user("poster.type.image@example.com"),
                POST_ID,
                upload("animation.gif", "image/gif", 7L)
        ))
                .isInstanceOf(InvalidInvitePostImageException.class)
                .hasMessage("File type is not supported");

        verifyNoInteractions(posts, images, storageService);
    }

    @Test
    void uploadImageRejectsOversizedFileBeforeLookingUpPost() {
        assertThatThrownBy(() -> service.uploadImage(
                user("poster.size.image@example.com"),
                POST_ID,
                upload("large.png", "image/png", 5_242_881L)
        ))
                .isInstanceOf(InvalidInvitePostImageException.class)
                .hasMessage("File size exceeds the maximum allowed invite post image size");

        verifyNoInteractions(posts, images, storageService);
    }

    @Test
    void uploadImageRejectsNonPosterBeforeStorage() {
        AppUser poster = user("poster.permission.image@example.com");
        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.of(post(poster, NOW.minusSeconds(60))));

        assertThatThrownBy(() -> service.uploadImage(
                user("outsider.permission.image@example.com"),
                POST_ID,
                upload("flyer.png", "image/png", 7L)
        ))
                .isInstanceOf(InvitePostImageForbiddenException.class)
                .hasMessage("Only the invite post poster can upload images");

        verifyNoInteractions(images, storageService);
    }

    @Test
    void uploadImageRejectsMissingPostBeforeStorage() {
        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadImage(
                user("poster.missing.image@example.com"),
                POST_ID,
                upload("flyer.png", "image/png", 7L)
        ))
                .isInstanceOf(InvitePostImageNotFoundException.class)
                .hasMessage("Invite post not found");

        verifyNoInteractions(images, storageService);
    }

    @Test
    void uploadImageRejectsClosedPostBeforeStorage() {
        AppUser poster = user("poster.closed.image@example.com");
        InvitePost post = post(poster, NOW.minusSeconds(60));
        post.close();
        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.uploadImage(
                poster,
                POST_ID,
                upload("flyer.png", "image/png", 7L)
        ))
                .isInstanceOf(InvalidInvitePostImageException.class)
                .hasMessage("Images can only be uploaded to active invite posts");

        verifyNoInteractions(images, storageService);
    }

    @Test
    void uploadImageRejectsExpiredPostBeforeStorage() {
        AppUser poster = user("poster.expired.image@example.com");
        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.of(post(
                poster,
                NOW.minusSeconds(24 * 60L * 60L)
        )));

        assertThatThrownBy(() -> service.uploadImage(
                poster,
                POST_ID,
                upload("flyer.png", "image/png", 7L)
        ))
                .isInstanceOf(InvalidInvitePostImageException.class)
                .hasMessage("Images cannot be uploaded to an expired invite post");

        verifyNoInteractions(images, storageService);
    }

    @Test
    void uploadImageRejectsFifthImageBeforeStorage() {
        AppUser poster = user("poster.limit.image@example.com");
        when(posts.findByIdForImageUpload(POST_ID)).thenReturn(Optional.of(post(poster, NOW.minusSeconds(60))));
        when(images.countByInvitePostId(POST_ID)).thenReturn(4L);

        assertThatThrownBy(() -> service.uploadImage(
                poster,
                POST_ID,
                upload("fifth.png", "image/png", 7L)
        ))
                .isInstanceOf(TooManyInvitePostImagesException.class)
                .hasMessage("An invite post can have at most 4 images");

        verifyNoInteractions(storageService);
    }

    @Test
    void getImageResponsesBulkLoadsImagesAndUsesPostExpiration() {
        InvitePost post = mock(InvitePost.class);
        InvitePostImage image = mock(InvitePostImage.class);
        Instant expiresAt = NOW.plusSeconds(1200);
        StorageAccessUrl accessUrl = new StorageAccessUrl(
                URI.create("https://example.com/view/post-image"),
                expiresAt
        );

        when(post.getId()).thenReturn(POST_ID);
        when(post.getStatus()).thenReturn(InvitePostStatus.ACTIVE);
        when(post.getExpiresAt()).thenReturn(expiresAt);
        when(post.isExpired(NOW)).thenReturn(false);
        when(image.getInvitePost()).thenReturn(post);
        when(image.getS3Bucket()).thenReturn("bucket");
        when(image.getS3ObjectKey()).thenReturn("key");
        when(image.getOriginalFilename()).thenReturn("flyer.webp");
        when(image.getContentType()).thenReturn("image/webp");
        when(image.getFileSizeBytes()).thenReturn(12L);
        when(image.getDisplayOrder()).thenReturn(1);
        when(image.getCreatedAt()).thenReturn(NOW.minusSeconds(30));
        when(images.findByInvitePostIdInOrderByInvitePostIdAscDisplayOrderAsc(any()))
                .thenReturn(List.of(image));
        when(storageService.generateViewUrl("bucket", "key", expiresAt)).thenReturn(accessUrl);

        var result = service.getImageResponses(List.of(post));

        assertThat(result).containsOnlyKeys(POST_ID);
        assertThat(result.get(POST_ID)).singleElement().satisfies(response -> {
            assertThat(response.url()).isEqualTo("https://example.com/view/post-image");
            assertThat(response.urlExpiresAt()).isEqualTo(expiresAt);
            assertThat(response.displayOrder()).isEqualTo(1);
        });
        verify(storageService).generateViewUrl("bucket", "key", expiresAt);
    }

    private InvitePostImageUpload upload(String filename, String contentType, long fileSizeBytes) {
        return new InvitePostImageUpload(
                filename,
                contentType,
                fileSizeBytes,
                new ByteArrayInputStream("content".getBytes())
        );
    }

    private InvitePost post(AppUser poster, Instant createdAt) {
        return new InvitePost(
                poster,
                "Community picnic",
                InviteType.GROUP,
                4,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                createdAt
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

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.getInvitePostImages().setMaxFileSizeBytes(5_242_880L);
        properties.getInvitePostImages().setAllowedContentTypes(List.of(
                "image/jpeg",
                "image/png",
                "image/webp"
        ));
        properties.getInvitePostImages().setViewUrlExpirationMinutes(60);
        return properties;
    }

    private InvitePostImageProperties imageProperties() {
        InvitePostImageProperties properties = new InvitePostImageProperties();
        properties.setMaxImagesPerPost(4);
        return properties;
    }
}
