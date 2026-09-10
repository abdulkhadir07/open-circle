package com.opencircle.profileimage;

import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageService;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProfileImageQueryServiceTest {

    private final ProfileImageRepository images = mock(ProfileImageRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final ProfileImageQueryService service = new ProfileImageQueryService(images, storageService);

    @Test
    void bulkLookupUsesOneRepositoryQueryAndMapsResponsesByUserId() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        ProfileImage firstImage = image(firstUserId, "first-key");
        ProfileImage secondImage = image(secondUserId, "second-key");
        Set<UUID> userIds = Set.of(firstUserId, secondUserId);

        when(images.findAllByUser_IdIn(userIds)).thenReturn(List.of(firstImage, secondImage));
        when(storageService.generateViewUrl("profile-bucket", "first-key"))
                .thenReturn(accessUrl("https://example.com/first"));
        when(storageService.generateViewUrl("profile-bucket", "second-key"))
                .thenReturn(accessUrl("https://example.com/second"));

        Map<UUID, ProfileImageResponse> result = service.getProfileImagesByUserIds(userIds);

        assertThat(result).containsOnlyKeys(firstUserId, secondUserId);
        assertThat(result.get(firstUserId).url()).isEqualTo("https://example.com/first");
        assertThat(result.get(secondUserId).url()).isEqualTo("https://example.com/second");
        verify(images).findAllByUser_IdIn(userIds);
    }

    @Test
    void bulkLookupSkipsDatabaseAndStorageForEmptyInput() {
        assertThat(service.getProfileImagesByUserIds(Set.of())).isEmpty();

        verifyNoInteractions(images, storageService);
    }

    private ProfileImage image(UUID userId, String objectKey) {
        ProfileImage image = mock(ProfileImage.class);
        when(image.getId()).thenReturn(UUID.randomUUID());
        when(image.getUserId()).thenReturn(userId);
        when(image.getContentType()).thenReturn("image/png");
        when(image.getS3Bucket()).thenReturn("profile-bucket");
        when(image.getS3ObjectKey()).thenReturn(objectKey);
        when(image.getUpdatedAt()).thenReturn(Instant.parse("2026-09-09T12:00:00Z"));
        return image;
    }

    private StorageAccessUrl accessUrl(String url) {
        return new StorageAccessUrl(
                URI.create(url),
                Instant.parse("2026-09-09T13:00:00Z")
        );
    }
}
