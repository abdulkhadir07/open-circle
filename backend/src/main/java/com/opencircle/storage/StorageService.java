package com.opencircle.storage;

import java.io.InputStream;

public interface StorageService {

    StoredFile upload(String keyPrefix, InputStream inputStream, long fileSizeBytes, String contentType);

    AttachmentDownloadUrl generateDownloadUrl(String bucket, String key, String downloadFilename);
}