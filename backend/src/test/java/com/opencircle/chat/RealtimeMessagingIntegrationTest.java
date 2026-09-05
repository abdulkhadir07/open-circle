package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RealtimeMessagingIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    @LocalServerPort
    private int port;

    @Autowired
    private UserService users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private ChatRoomRepository rooms;

    @Autowired
    private ChatMessageRepository messages;

    private final List<StompSession> sessions = new ArrayList<>();
    private final List<WebSocketStompClient> clients = new ArrayList<>();
    private final List<ChatRoom> createdRooms = new ArrayList<>();
    private final List<InvitePost> createdPosts = new ArrayList<>();

    @AfterEach
    void cleanup() {
        sessions.forEach(session -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        clients.forEach(WebSocketStompClient::stop);

        createdRooms.forEach(room ->
                rooms.findById(room.getId()).ifPresent(managedRoom -> {
                    messages.deleteAll(messages.findByChatRoomOrderByCreatedAtAscIdAsc(managedRoom));
                    rooms.delete(managedRoom);
                })
        );

        createdPosts.forEach(post -> posts.deleteById(post.getId()));
    }

    @Test
    void participantCanSendAndReceiveRealtimeMessage() throws Exception {
        AppUser poster = verifiedUser("poster.realtime");
        AppUser requester = verifiedUser("requester.realtime");
        ChatRoom room = chatRoom(poster, requester, "Realtime chat");

        StompSession session = connectAs(requester);
        BlockingQueue<Map<String, Object>> receivedMessages = new LinkedBlockingQueue<>();

        session.subscribe(
                "/topic/chat-rooms/" + room.getId(),
                mapFrameHandler(receivedMessages)
        );

        session.send(
                "/app/chat-rooms/" + room.getId() + "/messages",
                Map.of("body", "Hello over WebSocket")
        );

        Map<String, Object> response = receivedMessages.poll(5, TimeUnit.SECONDS);

        assertThat(response).isNotNull();
        assertThat(response.get("roomId")).isEqualTo(room.getId().toString());
        assertThat(response.get("senderId")).isEqualTo(requester.getId().toString());
        assertThat(response.get("senderUsername")).isEqualTo(requester.getUsername());
        assertThat(response.get("body")).isEqualTo("Hello over WebSocket");
    }

    @Test
    void nonParticipantSendReceivesRealtimeError() throws Exception {
        AppUser poster = verifiedUser("poster.error");
        AppUser requester = verifiedUser("requester.error");
        AppUser outsider = verifiedUser("outsider.error");
        ChatRoom room = chatRoom(poster, requester, "Realtime forbidden chat");

        StompSession session = connectAs(outsider);
        BlockingQueue<Map<String, Object>> errors = new LinkedBlockingQueue<>();

        session.subscribe(
                "/user/queue/errors",
                mapFrameHandler(errors)
        );

        session.send(
                "/app/chat-rooms/" + room.getId() + "/messages",
                Map.of("body", "I should not be able to send this")
        );

        Map<String, Object> error = errors.poll(5, TimeUnit.SECONDS);

        assertThat(error).isNotNull();
        assertThat(error.get("status")).isEqualTo(403);
        assertThat(error.get("error")).isEqualTo("FORBIDDEN");
        assertThat(error.get("message")).isEqualTo("You must be a chat participant to perform this action");
    }

    private StompSession connectAs(AppUser user) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.start();
        clients.add(client);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtService.generateToken(user));

        StompSession session = client.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);

        sessions.add(session);
        return session;
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

    private ChatRoom chatRoom(AppUser poster, AppUser requester, String content) {
        InvitePost post = posts.save(new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                Instant.now()
        ));

        createdPosts.add(post);

        ChatRoom room = chatRoomService.openRoomForAcceptedRequest(post, requester);
        createdRooms.add(room);

        return room;
    }

    private AppUser verifiedUser(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        String email = label + "." + unique + "@example.com";

        AppUser user = users.createUser(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                "+1415" + unique,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        user.markEmailVerified(NOW);
        user.verifyLocation("San Francisco", "California", "USA", NOW);

        return users.save(user);
    }
}