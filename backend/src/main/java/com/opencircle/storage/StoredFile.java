package com.opencircle.storage;

public record StoredObject(
        String bucket,
        String objectKey,
        String contentType,
        long fileSizeBytes
) {

    public StoredObject {
        bucket = requiredText(bucket, "S3 bucket is required");
        objectKey = requiredText(objectKey, "S3 object key is required");
        contentType = requiredText(contentType, "Content type is required");

        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("File size must be greater than zero");
        }
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}