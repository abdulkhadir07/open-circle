package com.opencircle.chat;

import java.io.InputStream;

record ChatAttachmentUpload(
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        InputStream inputStream,
        String caption
) {
}