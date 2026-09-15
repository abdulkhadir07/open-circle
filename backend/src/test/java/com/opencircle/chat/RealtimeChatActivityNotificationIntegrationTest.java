package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.security.JwtService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealtimeChatActivityNotificationIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort private int port;

    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private ChatRoomRepository rooms;
    @Autowired private ChatMessageRepository messages;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatRoomPresence presence;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private ProfileImageQueryService profileImages;

    private StompSession session;
    private WebSocketStompClient client;
    private ChatRoom room;
    private InvitePost post;

    @AfterEach
    void cleanup() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        if (client != null) {
            client.stop();
        }
        if (room != null) {
            jdbc.update("DELETE FROM notifications WHERE resource_type = 'CHAT_ROOM' AND resource_id = ?", room.getId());
            rooms.findById(room.getId()).ifPresent(managedRoom -> {
                messages.deleteAll(messages.findByChatRoomOrderByCreatedAtAscIdAsc(managedRoom));
                rooms.delete(managedRoom);
            });
        }
        if (post != null) {
            posts.deleteById(post.getId());
        }
    }

    @Test
    void roomSubscriptionSuppressesNotificationUntilRecipientUnsubscribes() throws Exception {
        when(profileImages.getProfileImagesByUserIds(any())).thenReturn(Map.of());
        AppUser sender = user("presence-sender");
        AppUser recipient = user("presence-recipient");
        room = room(sender, recipient);
        session = connectAs(recipient);
        BlockingQueue<Map<String, Object>> deliveries = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", mapFrameHandler(deliveries));
        StompSession.Subscription roomSubscription = session.subscribe(
                "/topic/chat-rooms/" + room.getId(),
                mapFrameHandler(new LinkedBlockingQueue<>())
        );
        awaitPresence(recipient.getId(), room.getId(), true);

        chatRoomService.sendMessage(sender, room.getId(), "Seen in the open room");

        assertThat(deliveries.poll(300, TimeUnit.MILLISECONDS)).isNull();
        assertThat(notificationCount(recipient.getId(), room.getId())).isZero();

        roomSubscription.unsubscribe();
        awaitPresence(recipient.getId(), room.getId(), false);
        chatRoomService.sendMessage(sender, room.getId(), "Sent while away");

        Map<String, Object> delivery = deliveries.poll(5, TimeUnit.SECONDS);
        assertThat(delivery).isNotNull();
        assertThat(delivery.get("notification"))
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("type", "CHAT_ACTIVITY")
                .containsEntry("occurrenceCount", 1)
                .satisfies(notification -> assertThat(notification.get("resource"))
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                        .containsEntry("type", "CHAT_ROOM")
                        .containsEntry("id", room.getId().toString()));
        assertThat(notificationCount(recipient.getId(), room.getId())).isEqualTo(1);
    }

    private void awaitPresence(UUID userId, UUID roomId, boolean expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (presence.isPresent(userId, roomId) != expected && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        assertThat(presence.isPresent(userId, roomId)).isEqualTo(expected);
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

    private ChatRoom room(AppUser sender, AppUser recipient) {
        post = posts.save(new InvitePost(
                sender,
                "Realtime activity notification",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                Instant.now().minusSeconds(60)
        ));
        return chatRoomService.openRoomForAcceptedRequest(post, recipient);
    }

    private int notificationCount(UUID recipientUserId, UUID roomId) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM notifications
                WHERE recipient_user_id = ?
                  AND type = 'CHAT_ACTIVITY'
                  AND resource_type = 'CHAT_ROOM'
                  AND resource_id = ?
                """,
                Integer.class,
                recipientUserId,
                roomId
        );
    }

    private AppUser user(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                label + "." + unique + "@example.com",
                "hashed-password",
                "+1%010d".formatted(Integer.toUnsignedLong((label + unique).hashCode()) % 10_000_000_000L),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
