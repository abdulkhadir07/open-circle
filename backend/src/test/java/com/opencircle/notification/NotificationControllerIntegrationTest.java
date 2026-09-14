package com.opencircle.notification;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.JwtService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NotificationPublisher publisher;

    @Autowired
    private NotificationRepository notifications;

    @MockitoBean
    private ProfileImageQueryService profileImages;

    @BeforeEach
    void defaultMissingProfileImages() {
        when(profileImages.getProfileImagesByUserIds(any())).thenReturn(Map.of());
    }

    @Test
    void inboxReturnsNewestSemanticNotificationsWithBatchedActorImages() throws Exception {
        AppUser recipient = user("inbox-recipient");
        AppUser firstActor = user("inbox-first-actor");
        AppUser secondActor = user("inbox-second-actor");
        UUID olderResourceId = UUID.randomUUID();
        UUID newerResourceId = UUID.randomUUID();
        ProfileImageResponse firstImage = profileImage("https://example.com/first-actor.png");
        ProfileImageResponse secondImage = profileImage("https://example.com/second-actor.png");
        Set<UUID> actorIds = Set.of(firstActor.getId(), secondActor.getId());
        when(profileImages.getProfileImagesByUserIds(actorIds)).thenReturn(Map.of(
                firstActor.getId(), firstImage,
                secondActor.getId(), secondImage
        ));

        publisher.publish(command(
                recipient,
                firstActor,
                NotificationType.ENGAGEMENT_REQUESTED,
                olderResourceId,
                NOW.minusSeconds(60)
        ));
        publisher.publish(command(
                recipient,
                secondActor,
                NotificationType.ENGAGEMENT_ACCEPTED,
                newerResourceId,
                NOW
        ));

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(2))
                .andExpect(jsonPath("$.notifications[0].type").value("ENGAGEMENT_ACCEPTED"))
                .andExpect(jsonPath("$.notifications[0].actor.userId")
                        .value(secondActor.getId().toString()))
                .andExpect(jsonPath("$.notifications[0].actor.username")
                        .value(secondActor.getUsername()))
                .andExpect(jsonPath("$.notifications[0].actor.profileImage.url")
                        .value("https://example.com/second-actor.png"))
                .andExpect(jsonPath("$.notifications[0].resource.type")
                        .value("ENGAGEMENT_REQUEST"))
                .andExpect(jsonPath("$.notifications[0].resource.id")
                        .value(newerResourceId.toString()))
                .andExpect(jsonPath("$.notifications[0].context.type").value("INVITE_POST"))
                .andExpect(jsonPath("$.notifications[0].occurrenceCount").value(1))
                .andExpect(jsonPath("$.notifications[0].read").value(false))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(profileImages).getProfileImagesByUserIds(actorIds);
    }

    @Test
    void unreadCountAndMarkOneAreOwnedAndIdempotent() throws Exception {
        AppUser recipient = user("read-recipient");
        AppUser otherUser = user("read-other");
        AppUser actor = user("read-actor");
        publisher.publish(command(recipient, actor, NotificationType.ENGAGEMENT_REQUESTED,
                UUID.randomUUID(), NOW.minusSeconds(10)));
        publisher.publish(command(recipient, actor, NotificationType.ENGAGEMENT_HELD,
                UUID.randomUUID(), NOW));

        UUID notificationId = notifications
                .findInbox(recipient.getId(), PageRequest.of(0, 20))
                .getContent()
                .getFirst()
                .getId();

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void markAllReadOnlyChangesCurrentUsersInbox() throws Exception {
        AppUser recipient = user("read-all-recipient");
        AppUser otherUser = user("read-all-other");
        AppUser actor = user("read-all-actor");
        publisher.publish(command(recipient, actor, NotificationType.ENGAGEMENT_ACCEPTED,
                UUID.randomUUID(), NOW));
        publisher.publish(command(otherUser, actor, NotificationType.ENGAGEMENT_DECLINED,
                UUID.randomUUID(), NOW));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", bearer(otherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void inboxRejectsInvalidPagination() throws Exception {
        AppUser recipient = user("page-recipient");

        mockMvc.perform(get("/api/notifications")
                        .param("size", "101")
                        .header("Authorization", bearer(recipient)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"));
    }

    @Test
    void notificationEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    private NotificationCommand command(
            AppUser recipient,
            AppUser actor,
            NotificationType type,
            UUID resourceId,
            Instant occurredAt
    ) {
        return new NotificationCommand(
                recipient.getId(),
                actor.getId(),
                type,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                resourceId,
                NotificationResourceType.INVITE_POST,
                UUID.randomUUID(),
                occurredAt
        );
    }

    private String bearer(AppUser user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private ProfileImageResponse profileImage(String url) {
        return new ProfileImageResponse(
                UUID.randomUUID(),
                url,
                NOW.plusSeconds(3600),
                "image/png",
                NOW
        );
    }

    private AppUser user(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                label + "." + unique + "@example.com",
                "hashed-password",
                "+1415" + unique,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
