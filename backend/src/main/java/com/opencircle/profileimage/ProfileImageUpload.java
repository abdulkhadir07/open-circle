package com.opencircle.profileimage;

import java.io.InputStream;

record ProfileImageUpload(
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        InputStream inputStream
) {
}
