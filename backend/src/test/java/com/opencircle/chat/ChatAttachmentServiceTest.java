package com.opencircle.chat;

import com.opencircle.storage.AttachmentDownloadUrl;
import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatAttachmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");
    private static final UUID ROOM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ATTACHMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ChatRoomService chatRoomService = mock(ChatRoomService.class);
    private final ChatAttachmentRepository attachments = mock(ChatAttachmentRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final StorageProperties storageProperties = storageProperties();

    private final ChatAttachmentService service = new ChatAttachmentService(
            chatRoomService,
            storageService,
            storageProperties,
            Clock.fixed(NOW, ZoneOffset.UTC),
            attachments
    );

    @Test
    void uploadAttachmentStoresFileAndCreatesAttachmentMessage() {
        AppUser sender = user("sender.upload@example.com");
        ChatRoom room = roomWithParticipants(sender, user("participant.upload@example.com"));

        when(chatRoomService.getRoomReadyForNewMessage(sender, ROOM_ID, NOW)).thenReturn(room);
        when(storageService.upload(eq("chat-attachments/chat-rooms/" + ROOM_ID), any(), eq(7L), eq("image/png")))
                .thenReturn(new StoredFile("bucket", "key", "image/png", 7L));
        when(chatRoomService.saveNewMessage(eq(room), any(ChatMessage.class), eq(NOW)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ChatMessage message = service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("receipt.png", "image/png", 7L, "Receipt photo")
        );

        assertThat(message.getType()).isEqualTo(ChatMessageType.ATTACHMENT);
        assertThat(message.getBody()).isEqualTo("Receipt photo");
        assertThat(message.getAttachment().getOriginalFilename()).isEqualTo("receipt.png");
        assertThat(message.getAttachment().getContentType()).isEqualTo("image/png");
        assertThat(message.getAttachment().getFileSizeBytes()).isEqualTo(7L);
        assertThat(message.getAttachment().getS3Bucket()).isEqualTo("bucket");
        assertThat(message.getAttachment().getS3ObjectKey()).isEqualTo("key");

        verify(storageService).upload(eq("chat-attachments/chat-rooms/" + ROOM_ID), any(), eq(7L), eq("image/png"));
        verify(chatRoomService).saveNewMessage(eq(room), any(ChatMessage.class), eq(NOW));
    }

    @Test
    void uploadAttachmentNormalizesBlankCaptionToNull() {
        AppUser sender = user("sender.blank.caption@example.com");
        ChatRoom room = roomWithParticipants(sender, user("participant.blank.caption@example.com"));

        when(chatRoomService.getRoomReadyForNewMessage(sender, ROOM_ID, NOW)).thenReturn(room);
        when(storageService.upload(eq("chat-attachments/chat-rooms/" + ROOM_ID), any(), eq(7L), eq("image/png")))
                .thenReturn(new StoredFile("bucket", "key", "image/png", 7L));
        when(chatRoomService.saveNewMessage(eq(room), any(ChatMessage.class), eq(NOW)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ChatMessage message = service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("receipt.png", "image/png", 7L, "   ")
        );

        assertThat(message.getBody()).isNull();
    }

    @Test
    void uploadAttachmentRejectsUnsupportedContentTypeBeforeStorage() {
        AppUser sender = user("sender.unsupported@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("notes.txt", "text/plain", 7L, null)
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("File type is not supported");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentRejectsOversizedFileBeforeStorage() {
        AppUser sender = user("sender.oversized@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("large.png", "image/png", 10_485_761L, null)
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("File size exceeds the maximum allowed attachment size");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentDoesNotStoreFileWhenRoomRejectsMessage() {
        AppUser sender = user("sender.room.reject@example.com");

        when(chatRoomService.getRoomReadyForNewMessage(sender, ROOM_ID, NOW))
                .thenThrow(new ChatParticipantRequiredException());

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("receipt.png", "image/png", 7L, null)
        ))
                .isInstanceOf(ChatParticipantRequiredException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void getDownloadUrlReturnsPresignedUrlForActiveParticipant() {
        AppUser requester = user("requester.download@example.com");
        AppUser otherParticipant = user("other.download@example.com");
        ChatRoom room = roomWithParticipants(requester, otherParticipant);
        ChatMessage message = ChatMessage.attachment(
                room,
                requester,
                "Receipt",
                "receipt.png",
                "image/png",
                7L,
                "bucket",
                "key",
                NOW
        );
        ChatAttachment attachment = message.getAttachment();
        AttachmentDownloadUrl downloadUrl = new AttachmentDownloadUrl(
                URI.create("https://example.com/download"),
                NOW.plusSeconds(600)
        );

        when(attachments.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
        when(chatRoomService.isActiveParticipant(requester, room.getId())).thenReturn(true);
        when(storageService.generateDownloadUrl("bucket", "key", "receipt.png")).thenReturn(downloadUrl);

        AttachmentDownloadUrl result = service.getDownloadUrl(requester, ATTACHMENT_ID);

        assertThat(result).isEqualTo(downloadUrl);
    }

    @Test
    void getDownloadUrlRejectsMissingAttachment() {
        AppUser requester = user("requester.missing@example.com");

        when(attachments.findById(ATTACHMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadUrl(requester, ATTACHMENT_ID))
                .isInstanceOf(ChatAttachmentNotFoundException.class)
                .hasMessage("Attachment not found");

        verifyNoInteractions(storageService);
    }

    @Test
    void getDownloadUrlRejectsNonParticipantBeforeGeneratingUrl() {
        AppUser requester = user("requester.forbidden@example.com");
        AppUser sender = user("sender.forbidden@example.com");
        ChatRoom room = roomWithParticipants(sender, user("participant.forbidden@example.com"));
        ChatMessage message = ChatMessage.attachment(
                room,
                sender,
                null,
                "receipt.png",
                "image/png",
                7L,
                "bucket",
                "key",
                NOW
        );

        when(attachments.findById(ATTACHMENT_ID)).thenReturn(Optional.of(message.getAttachment()));
        when(chatRoomService.isActiveParticipant(requester, room.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.getDownloadUrl(requester, ATTACHMENT_ID))
                .isInstanceOf(ChatParticipantRequiredException.class);

        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentRejectsMissingFileBeforeStorage() {
        AppUser sender = user("sender.missing.file@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                null
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("Attachment file is required");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentRejectsBlankFilenameBeforeStorage() {
        AppUser sender = user("sender.blank.filename@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("   ", "image/png", 7L, null)
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("Original filename is required");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentRejectsBlankContentTypeBeforeStorage() {
        AppUser sender = user("sender.blank.content.type@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("receipt.png", "   ", 7L, null)
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("Content type is required");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    @Test
    void uploadAttachmentRejectsTooLongCaptionBeforeStorage() {
        AppUser sender = user("sender.long.caption@example.com");

        assertThatThrownBy(() -> service.uploadAttachment(
                sender,
                ROOM_ID,
                upload("receipt.png", "image/png", 7L, "a".repeat(1001))
        ))
                .isInstanceOf(InvalidChatAttachmentException.class)
                .hasMessage("Caption cannot exceed 1000 characters");

        verifyNoInteractions(chatRoomService);
        verifyNoInteractions(storageService);
    }

    private ChatAttachmentUpload upload(String filename, String contentType, long fileSizeBytes, String caption) {
        return new ChatAttachmentUpload(
                filename,
                contentType,
                fileSizeBytes,
                new ByteArrayInputStream("content".getBytes()),
                caption
        );
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.getAttachments().setMaxFileSizeBytes(10_485_760L);
        properties.getAttachments().setAllowedContentTypes(java.util.List.of(
                "image/jpeg",
                "image/png",
                "image/webp",
                "application/pdf"
        ));
        return properties;
    }

    private ChatRoom roomWithParticipants(AppUser first, AppUser second) {
        ChatRoom room = new ChatRoom(invitePost(first), NOW);
        room.addParticipant(first, NOW);
        room.addParticipant(second, NOW.plusSeconds(1));
        return room;
    }

    private com.opencircle.invitepost.InvitePost invitePost(AppUser poster) {
        poster.markEmailVerified(NOW);
        poster.verifyLocation("San Francisco", "California", "USA", NOW);

        return new com.opencircle.invitepost.InvitePost(
                poster,
                "Anyone want coffee?",
                com.opencircle.invitepost.InviteType.GROUP,
                3,
                com.opencircle.invitepost.LocationScope.CITY,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                NOW
        );
    }

    private AppUser user(String email) {
        return new AppUser(
                "test_" + Math.abs(email.hashCode()),
                "Test",
                "User",
                email,
                "hashed-password",
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}