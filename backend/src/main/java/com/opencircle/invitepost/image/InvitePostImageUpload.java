package com.opencircle.invitepost.image;

import java.io.InputStream;

record InvitePostImageUpload(
        String originalFilename,
        String contentType,
        long fileSizeBytes,
        InputStream inputStream
) {
}
