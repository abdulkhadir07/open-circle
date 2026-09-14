package com.opencircle.notification;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.JwtService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealtimeNotificationIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserService users;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NotificationPublisher publisher;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private TransactionTemplate transactions;

    @MockitoBean
    private ProfileImageQueryService profileImages;

    private StompSession session;
    private WebSocketStompClient client;

    @AfterEach
    void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (client != null) {
            client.stop();
        }
        notifications.deleteAll();
    }

    @Test
    void recipientReceivesPrivateNotificationAfterCommit() throws Exception {
        AppUser recipient = user("realtime-recipient");
        AppUser actor = user("realtime-actor");
        UUID engagementId = UUID.randomUUID();
        ProfileImageResponse profileImage = new ProfileImageResponse(
                UUID.randomUUID(),
                "https://example.com/realtime-actor.png",
                Instant.now().plusSeconds(3600),
                "image/png",
                Instant.now()
        );
        when(profileImages.getProfileImagesByUserIds(Set.of(actor.getId())))
                .thenReturn(Map.of(actor.getId(), profileImage));

        session = connectAs(recipient);
        BlockingQueue<Map<String, Object>> deliveries = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", mapFrameHandler(deliveries));

        transactions.executeWithoutResult(status -> publisher.publish(new NotificationCommand(
                recipient.getId(),
                actor.getId(),
                NotificationType.ENGAGEMENT_REQUESTED,
                NotificationResourceType.ENGAGEMENT_REQUEST,
                engagementId,
                NotificationResourceType.INVITE_POST,
                UUID.randomUUID(),
                Instant.now()
        )));

        Map<String, Object> delivery = deliveries.poll(5, TimeUnit.SECONDS);

        assertThat(delivery).isNotNull();
        assertThat(delivery.get("unreadCount")).isEqualTo(1);
        assertThat(delivery.get("notification"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("type", "ENGAGEMENT_REQUESTED")
                .satisfies(notification -> {
                    assertThat(notification.get("actor"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("userId", actor.getId().toString())
                            .containsEntry("username", actor.getUsername())
                            .satisfies(actorResponse -> assertThat(actorResponse.get("profileImage"))
                                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                                    .containsEntry("url", "https://example.com/realtime-actor.png"));
                    assertThat(notification.get("resource"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsEntry("type", "ENGAGEMENT_REQUEST")
                            .containsEntry("id", engagementId.toString());
                });
    }

    private StompSession connectAs(AppUser user) throws Exception {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.start();

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(user));

        return client.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    private StompFrameHandler mapFrameHandler(BlockingQueue<Map<String, Object>> queue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        };
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
