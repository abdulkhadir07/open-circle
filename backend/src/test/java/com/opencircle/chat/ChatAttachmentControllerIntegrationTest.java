package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.storage.StorageAccessUrl;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatAttachmentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");

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

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private ChatMessageBroadcaster messageBroadcaster;

    @Test
    void uploadAttachmentCreatesAttachmentMessageAndBroadcastsIt() throws Exception {
        AppUser poster = verifiedUser("poster.attachment.upload@example.com");
        AppUser requester = verifiedUser("requester.attachment.upload@example.com");
        ChatRoom room = chatRoom(poster, requester, "Attachment upload room");
        String token = loginToken(requester.getEmail());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "content".getBytes()
        );

        when(storageService.upload(
                eq("chat-attachments/chat-rooms/" + room.getId()),
                any(),
                eq(7L),
                eq("image/png")
        )).thenReturn(new StoredFile(
                "opencircle-test-attachments",
                "chat-attachments/chat-rooms/" + room.getId() + "/stored-file",
                "image/png",
                7L
        ));

        mockMvc.perform(multipart("/api/chat-rooms/{roomId}/attachments", room.getId())
                        .file(file)
                        .param("caption", "Receipt photo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(room.getId().toString()))
                .andExpect(jsonPath("$.senderId").value(requester.getId().toString()))
                .andExpect(jsonPath("$.type").value("ATTACHMENT"))
                .andExpect(jsonPath("$.body").value("Receipt photo"))
                .andExpect(jsonPath("$.attachment.id").exists())
                .andExpect(jsonPath("$.attachment.originalFilename").value("receipt.png"))
                .andExpect(jsonPath("$.attachment.contentType").value("image/png"))
                .andExpect(jsonPath("$.attachment.fileSizeBytes").value(7));

        ArgumentCaptor<ChatMessage> broadcastedMessageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageBroadcaster).broadcast(broadcastedMessageCaptor.capture());

        ChatMessage broadcastedMessage = broadcastedMessageCaptor.getValue();
        ChatMessageResponse broadcastedResponse = ChatMessageResponse.from(broadcastedMessage);

        assertThat(broadcastedMessage.getType()).isEqualTo(ChatMessageType.ATTACHMENT);
        assertThat(broadcastedMessage.getBody()).isEqualTo("Receipt photo");
        assertThat(broadcastedMessage.getAttachment()).isNotNull();

        assertThat(broadcastedResponse.roomId()).isEqualTo(room.getId());
        assertThat(broadcastedResponse.senderId()).isEqualTo(requester.getId());
        assertThat(broadcastedResponse.type()).isEqualTo(ChatMessageType.ATTACHMENT);
        assertThat(broadcastedResponse.body()).isEqualTo("Receipt photo");
        assertThat(broadcastedResponse.attachment()).isNotNull();
        assertThat(broadcastedResponse.attachment().originalFilename()).isEqualTo("receipt.png");
        assertThat(broadcastedResponse.attachment().contentType()).isEqualTo("image/png");
        assertThat(broadcastedResponse.attachment().fileSizeBytes()).isEqualTo(7L);

        inTransaction(() -> {
            ChatRoom managedRoom = rooms.findById(room.getId()).orElseThrow();
            ChatMessage savedMessage = messages.findByChatRoomOrderByCreatedAtAscIdAsc(managedRoom)
                    .getFirst();

            assertThat(savedMessage.getType()).isEqualTo(ChatMessageType.ATTACHMENT);
            assertThat(savedMessage.getBody()).isEqualTo("Receipt photo");
            assertThat(savedMessage.getAttachment().getOriginalFilename()).isEqualTo("receipt.png");
            assertThat(savedMessage.getAttachment().getContentType()).isEqualTo("image/png");
            assertThat(savedMessage.getAttachment().getFileSizeBytes()).isEqualTo(7L);
            assertThat(savedMessage.getAttachment().getS3Bucket()).isEqualTo("opencircle-test-attachments");
            assertThat(savedMessage.getAttachment().getS3ObjectKey())
                    .isEqualTo("chat-attachments/chat-rooms/" + room.getId() + "/stored-file");

            return null;
        });
    }

    @Test
    void uploadAttachmentRejectsUnsupportedFileTypeWithoutCallingStorage() throws Exception {
        AppUser poster = verifiedUser("poster.attachment.type@example.com");
        AppUser requester = verifiedUser("requester.attachment.type@example.com");
        ChatRoom room = chatRoom(poster, requester, "Unsupported upload room");
        String token = loginToken(requester.getEmail());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/chat-rooms/{roomId}/attachments", room.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File type is not supported"));

        verifyNoInteractions(storageService);
        verifyNoInteractions(messageBroadcaster);
    }

    @Test
    void getDownloadUrlReturnsPresignedUrlForParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.attachment.download@example.com");
        AppUser requester = verifiedUser("requester.attachment.download@example.com");
        ChatRoom room = chatRoom(poster, requester, "Download room");
        UUID attachmentId = attachmentMessage(room, requester);
        String token = loginToken(requester.getEmail());

        Instant expiresAt = NOW.plusSeconds(600);

        when(storageService.generateDownloadUrl(
                "opencircle-test-attachments",
                "chat-attachments/chat-rooms/" + room.getId() + "/menu.pdf",
                "menu.pdf"
        )).thenReturn(new StorageAccessUrl(
                URI.create("https://example.com/download/menu.pdf"),
                expiresAt
        ));

        mockMvc.perform(get("/api/attachments/{attachmentId}/download-url", attachmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/download/menu.pdf"))
                .andExpect(jsonPath("$.expiresAt").value(expiresAt.toString()));
    }

    @Test
    void getDownloadUrlRejectsNonParticipant() throws Exception {
        AppUser poster = verifiedUser("poster.attachment.forbidden@example.com");
        AppUser requester = verifiedUser("requester.attachment.forbidden@example.com");
        AppUser outsider = verifiedUser("outsider.attachment.forbidden@example.com");
        ChatRoom room = chatRoom(poster, requester, "Forbidden download room");
        UUID attachmentId = attachmentMessage(room, requester);
        String token = loginToken(outsider.getEmail());

        mockMvc.perform(get("/api/attachments/{attachmentId}/download-url", attachmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You must be a chat participant to perform this action"));

        verify(storageService, never()).generateDownloadUrl(any(), any(), any());
    }

    @Test
    void attachmentEndpointsRequireAuthentication() throws Exception {
        UUID roomId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                "content".getBytes()
        );

        mockMvc.perform(multipart("/api/chat-rooms/{roomId}/attachments", roomId)
                        .file(file))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/attachments/{attachmentId}/download-url", attachmentId))
                .andExpect(status().isUnauthorized());
    }

    private UUID attachmentMessage(ChatRoom room, AppUser sender) {
        return inTransaction(() -> {
            ChatRoom managedRoom = rooms.findById(room.getId()).orElseThrow();

            ChatMessage message = ChatMessage.attachment(
                    managedRoom,
                    sender,
                    "Menu",
                    "menu.pdf",
                    "application/pdf",
                    12L,
                    "opencircle-test-attachments",
                    "chat-attachments/chat-rooms/" + room.getId() + "/menu.pdf",
                    NOW.plusSeconds(30)
            );

            return messages.saveAndFlush(message).getAttachment().getId();
        });
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
