package com.opencircle.profileimage;

import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProfileImageQueryService {

    private final ProfileImageRepository images;
    private final StorageService storageService;

    ProfileImageQueryService(ProfileImageRepository images, StorageService storageService) {
        this.images = images;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Map<UUID, ProfileImageResponse> getProfileImagesByUserIds(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Set<UUID> distinctUserIds = new LinkedHashSet<>(userIds);
        distinctUserIds.remove(null);

        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ProfileImageResponse> responses = new LinkedHashMap<>();

        images.findAllByUser_IdIn(distinctUserIds).forEach(image ->
                responses.put(image.getUserId(), responseFor(image))
        );

        return Map.copyOf(responses);
    }

    @Transactional(readOnly = true)
    public ProfileImageResponse getProfileImageByUserId(UUID userId) {
        if (userId == null) {
            return null;
        }

        return images.findByUser_Id(userId)
                .map(this::responseFor)
                .orElse(null);
    }

    private ProfileImageResponse responseFor(ProfileImage image) {
        StorageAccessUrl accessUrl = storageService.generateViewUrl(
                image.getS3Bucket(),
                image.getS3ObjectKey()
        );

        return ProfileImageResponse.from(image, accessUrl);
    }
}
