package com.opencircle.storage;

public record StoredFile(
        String bucket,
        String key,
        String contentType,
        long fileSizeBytes
) {
}