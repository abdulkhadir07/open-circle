package com.opencircle.invitepost.image;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InvitePostStatus;
import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitePostImageService {

    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 120;

    private final InvitePostRepository posts;
    private final InvitePostImageRepository images;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final InvitePostImageProperties imageProperties;
    private final Clock clock;

    InvitePostImageService(
            InvitePostRepository posts,
            InvitePostImageRepository images,
            StorageService storageService,
            StorageProperties storageProperties,
            InvitePostImageProperties imageProperties,
            Clock clock
    ) {
        this.posts = posts;
        this.images = images;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.imageProperties = imageProperties;
        this.clock = clock;
    }

    @Transactional
    InvitePostImageResponse uploadImage(AppUser uploader, UUID postId, InvitePostImageUpload upload) {
        validate(upload);

        Instant now = Instant.now(clock);
        InvitePost post = posts.findByIdForImageUpload(postId)
                .orElseThrow(() -> new InvitePostImageNotFoundException("Invite post not found"));

        if (!sameUser(post.getPoster(), uploader)) {
            throw new InvitePostImageForbiddenException();
        }

        if (post.getStatus() != InvitePostStatus.ACTIVE) {
            throw new InvalidInvitePostImageException("Images can only be uploaded to active invite posts");
        }

        if (post.isExpired(now)) {
            throw new InvalidInvitePostImageException("Images cannot be uploaded to an expired invite post");
        }

        long currentImageCount = images.countByInvitePostId(postId);

        if (currentImageCount >= imageProperties.getMaxImagesPerPost()) {
            throw new TooManyInvitePostImagesException(imageProperties.getMaxImagesPerPost());
        }

        StoredFile storedFile = storageService.upload(
                storageKeyPrefix(postId),
                upload.inputStream(),
                upload.fileSizeBytes(),
                normalizedContentType(upload.contentType())
        );

        InvitePostImage image = images.save(new InvitePostImage(
                post,
                uploader,
                upload.originalFilename(),
                storedFile.contentType(),
                storedFile.fileSizeBytes(),
                storedFile.bucket(),
                storedFile.key(),
                Math.toIntExact(currentImageCount + 1),
                now
        ));

        return responseFor(image, post.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<InvitePostImageResponse>> getImageResponses(Collection<InvitePost> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }

        Instant now = Instant.now(clock);
        Map<UUID, Instant> activePostExpirations = new LinkedHashMap<>();

        posts.stream()
                .filter(post -> post.getStatus() == InvitePostStatus.ACTIVE)
                .filter(post -> !post.isExpired(now))
                .forEach(post -> activePostExpirations.put(post.getId(), post.getExpiresAt()));

        if (activePostExpirations.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<InvitePostImageResponse>> responses = new LinkedHashMap<>();

        images.findByInvitePostIdInOrderByInvitePostIdAscDisplayOrderAsc(activePostExpirations.keySet())
                .forEach(image -> {
                    UUID postId = image.getInvitePost().getId();
                    InvitePostImageResponse response = responseFor(image, activePostExpirations.get(postId));
                    responses.computeIfAbsent(postId, ignored -> new ArrayList<>()).add(response);
                });

        return responses;
    }

    private InvitePostImageResponse responseFor(InvitePostImage image, Instant postExpiresAt) {
        StorageAccessUrl accessUrl = storageService.generateViewUrl(
                image.getS3Bucket(),
                image.getS3ObjectKey(),
                postExpiresAt
        );

        return InvitePostImageResponse.from(image, accessUrl);
    }

    private void validate(InvitePostImageUpload upload) {
        if (upload == null || upload.inputStream() == null) {
            throw new InvalidInvitePostImageException("Image file is required");
        }

        if (upload.originalFilename() == null || upload.originalFilename().isBlank()) {
            throw new InvalidInvitePostImageException("Original filename is required");
        }

        if (upload.originalFilename().trim().length() > MAX_FILENAME_LENGTH) {
            throw new InvalidInvitePostImageException("Original filename is too long");
        }

        if (upload.contentType() == null || upload.contentType().isBlank()) {
            throw new InvalidInvitePostImageException("Content type is required");
        }

        if (upload.contentType().trim().length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new InvalidInvitePostImageException("Content type is too long");
        }

        if (upload.fileSizeBytes() <= 0) {
            throw new InvalidInvitePostImageException("File size must be greater than zero");
        }

        if (upload.fileSizeBytes() > storageProperties.getInvitePostImages().getMaxFileSizeBytes()) {
            throw new InvalidInvitePostImageException("File size exceeds the maximum allowed invite post image size");
        }

        if (!storageProperties.isAllowedInvitePostImageContentType(upload.contentType())) {
            throw new InvalidInvitePostImageException("File type is not supported");
        }
    }

    private String storageKeyPrefix(UUID postId) {
        return "invite-post-images/invite-posts/" + postId;
    }

    private String normalizedContentType(String contentType) {
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first != null
                && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }
}
