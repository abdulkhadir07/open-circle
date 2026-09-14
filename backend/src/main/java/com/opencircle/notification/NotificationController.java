package com.opencircle.notification;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationQueryService notificationQueryService;
    private final NotificationService notificationService;

    NotificationController(
            CurrentUserProvider currentUserProvider,
            NotificationQueryService notificationQueryService,
            NotificationService notificationService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.notificationQueryService = notificationQueryService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationInboxResponse getInbox(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        return notificationQueryService.getInbox(currentUser.getId(), page, size);
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        return new UnreadNotificationCountResponse(
                notificationQueryService.getUnreadCount(currentUser.getId())
        );
    }

    @PatchMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID notificationId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        notificationService.markRead(currentUser.getId(), notificationId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);
        notificationService.markAllRead(currentUser.getId());
    }
}
