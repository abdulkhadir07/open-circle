package com.opencircle.notification;

import com.opencircle.profileimage.ProfileImageResponse;

import java.util.UUID;

public record NotificationActorResponse(
        UUID userId,
        String username,
        ProfileImageResponse profileImage
) {
}
