package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatRoomControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService users;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private ChatRoomRepository rooms;

    @Autowired
    private ChatMessageRepository messages;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void getMyRoomsReturnsOnlyRoomsWhereCurrentUserIsParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.rooms@example.com");
        AppUser requester = verifiedUser("requester.rooms@example.com");
        AppUser outsider = verifiedUser("outsider.rooms@example.com");

        ChatRoom requesterRoom = chatRoom(poster, requester, "Coffee chat");
        chatRoom(poster, outsider, "Other chat");

        String token = loginToken(requester.getEmail());

        mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(requesterRoom.getId().toString()))
                .andExpect(jsonPath("$[0].invitePostContent").value("Coffee chat"))
                .andExpect(jsonPath("$[0].participants.length()").value(2));
    }

    @Test
    void getMessagesReturnsMessagesOldestFirstForParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.messages@example.com");
        AppUser requester = verifiedUser("requester.messages@example.com");
        ChatRoom room = chatRoom(poster, requester, "Message chat");

        inTransaction(() -> {
            ChatRoom managedRoom = rooms.findById(room.getId()).orElseThrow();
            messages.save(new ChatMessage(managedRoom, poster, "First message", NOW.minusSeconds(30)));
            messages.save(new ChatMessage(managedRoom, requester, "Second message", NOW.minusSeconds(10)));
            return null;
        });

        String token = loginToken(requester.getEmail());

        mockMvc.perform(get("/api/chat-rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("First message"))
                .andExpect(jsonPath("$[1].body").value("Second message"));
    }

    @Test
    void sendMessageCreatesMessageForParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.send@example.com");
        AppUser requester = verifiedUser("requester.send@example.com");
        ChatRoom room = chatRoom(poster, requester, "Send chat");

        String token = loginToken(requester.getEmail());

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Hello from requester"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
                .andExpect(jsonPath("$.senderId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.body").value("Hello from requester"));

        inTransaction(() -> {
            ChatRoom managedRoom = rooms.findById(room.getId()).orElseThrow();

            assertThat(messages.findByChatRoomOrderByCreatedAtAscIdAsc(managedRoom))
                    .extracting(ChatMessage::getBody)
                    .contains("Hello from requester");

            return null;
        });
    }

    @Test
    void sendMessageRejectsNonParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.forbidden@example.com");
        AppUser requester = verifiedUser("requester.forbidden@example.com");
        AppUser outsider = verifiedUser("outsider.forbidden@example.com");
        ChatRoom room = chatRoom(poster, requester, "Private chat");

        String token = loginToken(outsider.getEmail());

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "I should not be here"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You must be a chat participant to perform this action"));
    }

    @Test
    void sendMessageRejectsBlankBody() throws Exception {
        AppUser poster = verifiedUser("poster.blank@example.com");
        AppUser requester = verifiedUser("requester.blank@example.com");
        ChatRoom room = chatRoom(poster, requester, "Blank message chat");

        String token = loginToken(requester.getEmail());

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatEndpointsRequireAuthentication() throws Exception {
        UUID roomId = UUID.randomUUID();

        mockMvc.perform(get("/api/chat-rooms"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/chat-rooms/{roomId}/messages", roomId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/chat-rooms/{roomId}/messages", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Hello"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private ChatRoom chatRoom(AppUser poster, AppUser participant, String content) {
        return inTransaction(() -> {
            InvitePost post = posts.save(invitePost(poster, content));
            ChatRoom room = new ChatRoom(post, NOW);
            room.addParticipant(poster, NOW);
            room.addParticipant(participant, NOW.plusSeconds(1));
            return rooms.save(room);
        });
    }

    private InvitePost invitePost(AppUser poster, String content) {
        return new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                NOW.minusSeconds(60)
        );
    }

    private AppUser verifiedUser(String email) {
        return inTransaction(() -> {
            AppUser user = users.createUser(
                    "Test",
                    "User",
                    email,
                    passwordEncoder.encode("Password123!"),
                    "+1415555" + Math.abs(email.hashCode() % 10000),
                    LocalDate.of(2000, 1, 1),
                    "San Francisco",
                    "California",
                    "USA"
            );

            user.markEmailVerified(NOW);
            user.verifyLocation("San Francisco", "California", "USA", NOW);

            return user;
        });
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }

    private <T> T inTransaction(Supplier<T> supplier) {
        return new TransactionTemplate(transactionManager).execute(status -> supplier.get());
    }
}