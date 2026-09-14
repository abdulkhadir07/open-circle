package com.opencircle.notification;

import java.util.List;

public record NotificationInboxResponse(
        List<NotificationResponse> notifications,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
