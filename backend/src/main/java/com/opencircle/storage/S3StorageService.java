package com.opencircle.storage;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;
    private final Clock clock;

    S3StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties, Clock clock) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public StoredFile upload(String keyPrefix, InputStream inputStream, long fileSizeBytes, String contentType) {
        if (inputStream == null) {
            throw new IllegalArgumentException("File content is required");
        }

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }

        String bucket = properties.getS3().getBucket();
        String key = "%s/%s".formatted(cleanKeyPrefix(keyPrefix), UUID.randomUUID());
        String normalizedContentType = requiredText(contentType, "Content type is required");

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(normalizedContentType)
                .contentLength(fileSizeBytes)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, fileSizeBytes));
            return new StoredFile(bucket, key, normalizedContentType, fileSizeBytes);
        } catch (SdkException exception) {
            throw new StorageException("Unable to store uploaded file", exception);
        }
    }

    @Override
    public StorageAccessUrl generateDownloadUrl(String bucket, String key, String downloadFilename) {
        Duration expiration = Duration.ofMinutes(properties.getS3().getPresignedUrlExpirationMinutes());
        Instant expiresAt = Instant.now(clock).plus(expiration);

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(requiredText(bucket, "S3 bucket is required"))
                .key(requiredText(key, "S3 object key is required"))
                .responseContentDisposition("attachment; filename=\"" + safeFilename(downloadFilename) + "\"")
                .build();

        return presign(objectRequest, expiration, expiresAt, "Unable to create download URL");
    }

    @Override
    public StorageAccessUrl generateViewUrl(String bucket, String key, Instant notAfter) {
        Instant now = Instant.now(clock);
        Instant configuredExpiration = now.plus(Duration.ofMinutes(
                properties.getInvitePostImages().getViewUrlExpirationMinutes()
        ));
        Instant expiresAt = configuredExpiration.isBefore(notAfter) ? configuredExpiration : notAfter;

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("View URL expiration must be in the future");
        }

        Duration expiration = Duration.between(now, expiresAt);
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(requiredText(bucket, "S3 bucket is required"))
                .key(requiredText(key, "S3 object key is required"))
                .build();

        return presign(objectRequest, expiration, expiresAt, "Unable to create view URL");
    }

    private StorageAccessUrl presign(
            GetObjectRequest objectRequest,
            Duration expiration,
            Instant expiresAt,
            String failureMessage
    ) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(objectRequest)
                .build();

        try {
            return new StorageAccessUrl(
                    URI.create(s3Presigner.presignGetObject(presignRequest).url().toString()),
                    expiresAt
            );
        } catch (SdkException exception) {
            throw new StorageException(failureMessage, exception);
        }
    }

    private String cleanKeyPrefix(String keyPrefix) {
        String normalized = requiredText(keyPrefix, "Storage key prefix is required")
                .replace('\\', '/');

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Storage key prefix is invalid");
        }

        return normalized;
    }

    private String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }

        String normalized = filename.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');

        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }

        normalized = normalized
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");

        return normalized.isBlank() ? "attachment" : normalized;
    }
}