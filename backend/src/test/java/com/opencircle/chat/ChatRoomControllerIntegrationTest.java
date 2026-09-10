package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

    @Autowired private ChatRoomParticipantRepository participants;

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

    @MockitoBean
    private ProfileImageQueryService profileImageQueryService;

    @BeforeEach
    void defaultMissingProfileImages() {
        when(profileImageQueryService.getProfileImagesByUserIds(any())).thenReturn(Map.of());
    }

    @Test
    void getMyRoomsReturnsOnlyRoomsWhereCurrentUserIsParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.rooms@example.com");
        AppUser requester = verifiedUser("requester.rooms@example.com");
        AppUser outsider = verifiedUser("outsider.rooms@example.com");

        ChatRoom requesterRoom = chatRoom(poster, requester, "Coffee chat");
        chatRoom(poster, outsider, "Other chat");

        String token = loginToken(requester.getEmail());
        Set<UUID> participantIds = Set.of(poster.getId(), requester.getId());
        when(profileImageQueryService.getProfileImagesByUserIds(participantIds))
                .thenReturn(Map.of(
                        poster.getId(), profileImage("https://example.com/room-poster.png"),
                        requester.getId(), profileImage("https://example.com/room-requester.png")
                ));

        mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(requesterRoom.getId().toString()))
                .andExpect(jsonPath("$[0].invitePostContent").value("Coffee chat"))
                .andExpect(jsonPath("$[0].participants.length()").value(2))
                .andExpect(jsonPath(
                        "$[0].participants[?(@.userId == '%s')].profileImage.url".formatted(requester.getId())
                ).value(contains("https://example.com/room-requester.png")));

        verify(profileImageQueryService).getProfileImagesByUserIds(participantIds);
    }

    @Test
    void getMessagesReturnsMessagesOldestFirstForParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.messages@example.com");
        AppUser requester = verifiedUser("requester.messages@example.com");
        ChatRoom room = chatRoom(poster, requester, "Message chat");

        inTransaction(() -> {
            ChatRoom managedRoom = rooms.findById(room.getId()).orElseThrow();
            messages.save(ChatMessage.text(managedRoom, poster, "First message", NOW.minusSeconds(30)));
            messages.save(ChatMessage.text(managedRoom, requester, "Second message", NOW.minusSeconds(10)));
            return null;
        });

        String token = loginToken(requester.getEmail());
        Set<UUID> senderIds = Set.of(poster.getId(), requester.getId());
        when(profileImageQueryService.getProfileImagesByUserIds(senderIds))
                .thenReturn(Map.of(
                        poster.getId(), profileImage("https://example.com/message-poster.png"),
                        requester.getId(), profileImage("https://example.com/message-requester.png")
                ));

        mockMvc.perform(get("/api/chat-rooms/{roomId}/messages", room.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].body").value("First message"))
                .andExpect(jsonPath("$[0].senderProfileImage.url")
                        .value("https://example.com/message-poster.png"))
                .andExpect(jsonPath("$[1].body").value("Second message"))
                .andExpect(jsonPath("$[1].senderProfileImage.url")
                        .value("https://example.com/message-requester.png"));

        verify(profileImageQueryService).getProfileImagesByUserIds(senderIds);
    }

    @Test
    void sendMessageCreatesMessageForParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.send@example.com");
        AppUser requester = verifiedUser("requester.send@example.com");
        ChatRoom room = chatRoom(poster, requester, "Send chat");

        String token = loginToken(requester.getEmail());
        when(profileImageQueryService.getProfileImageByUserId(requester.getId()))
                .thenReturn(profileImage("https://example.com/sender.png"));

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
                .andExpect(jsonPath("$.senderProfileImage.url").value("https://example.com/sender.png"))
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

    @Test
    void saveRoomMarksRoomSaved() throws Exception {
        AppUser poster = verifiedUser("poster.save@example.com");
        AppUser requester = verifiedUser("requester.save@example.com");
        ChatRoom room = chatRoom(poster, requester, "Save this room");
        String token = loginToken(requester.getEmail());
        Set<UUID> participantIds = Set.of(poster.getId(), requester.getId());
        when(profileImageQueryService.getProfileImagesByUserIds(participantIds))
                .thenReturn(Map.of(
                        requester.getId(),
                        profileImage("https://example.com/saved-by.png")
                ));

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/save", room.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.savedByUserId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.savedByUsername").value(requester.getUsername()))
                .andExpect(jsonPath("$.savedByProfileImage.url").value("https://example.com/saved-by.png"))
                .andExpect(jsonPath("$.closed").value(false));

        ChatRoom savedRoom = rooms.findById(room.getId()).orElseThrow();

        assertThat(savedRoom.isSaved()).isTrue();
        assertThat(savedRoom.getSavedBy().getId()).isEqualTo(requester.getId());
        assertThat(savedRoom.getSavedAt()).isNotNull();
    }

    @Test
    void leaveRoomMarksCurrentUserInactiveAndStartsAutoCloseCountdown() throws Exception {
        AppUser poster = verifiedUser("poster.leave@example.com");
        AppUser requester = verifiedUser("requester.leave@example.com");
        ChatRoom room = chatRoom(poster, requester, "Leave this room");
        String token = loginToken(requester.getEmail());

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/leave", room.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoCloseAt").exists())
                .andExpect(jsonPath("$.closed").value(false));

        ChatRoomParticipant participant = participants.findByChatRoomAndUser(room, requester)
                .orElseThrow();

        ChatRoom updatedRoom = rooms.findById(room.getId()).orElseThrow();

        assertThat(participant.isActive()).isFalse();
        assertThat(participant.getLeftAt()).isNotNull();
        assertThat(updatedRoom.getAutoCloseAt()).isNotNull();
    }

    @Test
    void hideRoomRemovesRoomFromCurrentUsersRoomListOnly() throws Exception {
        AppUser poster = verifiedUser("poster.hide@example.com");
        AppUser requester = verifiedUser("requester.hide@example.com");
        ChatRoom room = chatRoom(poster, requester, "Hide this room");
        String requesterToken = loginToken(requester.getEmail());
        String posterToken = loginToken(poster.getEmail());

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/hide", room.getId())
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenForCurrentUser").value(true));

        mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/chat-rooms")
                        .header("Authorization", "Bearer " + posterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(room.getId().toString()));
    }

    @Test
    void posterCanRemoveParticipantFromRoom() throws Exception {
        AppUser poster = verifiedUser("poster.remove@example.com");
        AppUser requester = verifiedUser("requester.remove@example.com");
        ChatRoom room = chatRoom(poster, requester, "Remove participant");
        String token = loginToken(poster.getEmail());
        Set<UUID> participantIds = Set.of(poster.getId(), requester.getId());
        when(profileImageQueryService.getProfileImagesByUserIds(participantIds))
                .thenReturn(Map.of(
                        poster.getId(),
                        profileImage("https://example.com/removed-by.png")
                ));

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/participants/{userId}/remove", room.getId(), requester.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(false))
                .andExpect(jsonPath("$.autoCloseAt").exists())
                .andExpect(jsonPath(
                        "$.participants[?(@.userId == '%s')].removedByProfileImage.url"
                                .formatted(requester.getId())
                ).value(contains("https://example.com/removed-by.png")));

        ChatRoomParticipant removedParticipant = participants.findByChatRoomAndUser(room, requester)
                .orElseThrow();

        assertThat(removedParticipant.isActive()).isFalse();
        assertThat(removedParticipant.getRemovedAt()).isNotNull();
        assertThat(removedParticipant.getRemovedBy().getId()).isEqualTo(poster.getId());
    }

    @Test
    void nonPosterCannotRemoveParticipantFromRoom() throws Exception {
        AppUser poster = verifiedUser("poster.remove.forbidden@example.com");
        AppUser requester = verifiedUser("requester.remove.forbidden@example.com");
        AppUser otherUser = verifiedUser("other.remove.forbidden@example.com");
        ChatRoom room = chatRoom(poster, requester, "Remove forbidden");
        String token = loginToken(otherUser.getEmail());

        room.addParticipant(otherUser, NOW);
        rooms.save(room);

        mockMvc.perform(patch("/api/chat-rooms/{roomId}/participants/{userId}/remove", room.getId(), requester.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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

    private ProfileImageResponse profileImage(String url) {
        return new ProfileImageResponse(
                UUID.randomUUID(),
                url,
                Instant.parse("2026-09-09T13:00:00Z"),
                "image/png",
                Instant.parse("2026-09-09T12:00:00Z")
        );
    }
}
