package com.opencircle.storage;

import java.io.InputStream;
import java.time.Instant;

public interface StorageService {

    StoredFile upload(String keyPrefix, InputStream inputStream, long fileSizeBytes, String contentType);

    void delete(String bucket, String key);

    StorageAccessUrl generateDownloadUrl(String bucket, String key, String downloadFilename);

    StorageAccessUrl generateViewUrl(String bucket, String key);

    StorageAccessUrl generateViewUrl(String bucket, String key, Instant notAfter);
}