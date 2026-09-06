package com.opencircle.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    @Valid
    private final S3 s3 = new S3();

    @Valid
    private final Attachments attachments = new Attachments();

    public S3 getS3() {
        return s3;
    }

    public Attachments getAttachments() {
        return attachments;
    }

    public boolean isAllowedAttachmentContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String normalized = contentType.trim();

        return attachments.allowedContentTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(normalized));
    }

    public static class S3 {

        @NotBlank
        private String bucket;

        @NotBlank
        private String region;

        @Positive
        private int presignedUrlExpirationMinutes;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public int getPresignedUrlExpirationMinutes() {
            return presignedUrlExpirationMinutes;
        }

        public void setPresignedUrlExpirationMinutes(int presignedUrlExpirationMinutes) {
            this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
        }
    }

    public static class Attachments {

        @Positive
        private long maxFileSizeBytes;

        @NotEmpty
        private List<@NotBlank String> allowedContentTypes = new ArrayList<>();

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public List<String> getAllowedContentTypes() {
            return allowedContentTypes;
        }

        public void setAllowedContentTypes(List<String> allowedContentTypes) {
            this.allowedContentTypes = allowedContentTypes;
        }
    }
}