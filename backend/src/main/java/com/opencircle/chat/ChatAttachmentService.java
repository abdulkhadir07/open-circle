package com.opencircle.chat;

import com.opencircle.storage.StorageProperties;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import com.opencircle.storage.AttachmentDownloadUrl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
class ChatAttachmentService {

    private static final int MAX_CAPTION_LENGTH = 1000;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 120;

    private final ChatRoomService chatRoomService;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final Clock clock;
    private final ChatAttachmentRepository attachments;

    ChatAttachmentService(
            ChatRoomService chatRoomService,
            StorageService storageService,
            StorageProperties storageProperties,
            Clock clock,
            ChatAttachmentRepository attachments
    ) {
        this.chatRoomService = chatRoomService;
        this.storageService = storageService;
        this.storageProperties = storageProperties;
        this.clock = clock;
        this.attachments = attachments;
    }

    @Transactional
    public ChatMessage uploadAttachment(AppUser sender, UUID roomId, ChatAttachmentUpload upload) {
        validate(upload);

        Instant now = Instant.now(clock);
        ChatRoom room = chatRoomService.getRoomReadyForNewMessage(sender, roomId, now);

        StoredFile storedFile = storageService.upload(
                storageKeyPrefix(roomId),
                upload.inputStream(),
                upload.fileSizeBytes(),
                upload.contentType().trim()
        );

        ChatMessage message = ChatMessage.attachment(
                room,
                sender,
                upload.caption(),
                upload.originalFilename(),
                storedFile.contentType(),
                storedFile.fileSizeBytes(),
                storedFile.bucket(),
                storedFile.key(),
                now
        );

        return chatRoomService.saveNewMessage(room, message, now);
    }

    @Transactional(readOnly = true)
    public AttachmentDownloadUrl getDownloadUrl(AppUser requester, UUID attachmentId) {
        ChatAttachment attachment = attachments.findById(attachmentId)
                .orElseThrow(() -> new ChatAttachmentNotFoundException("Attachment not found"));

        if (!chatRoomService.isActiveParticipant(requester, attachment.getChatRoom().getId())) {
            throw new ChatParticipantRequiredException();
        }

        // Creates a short-lived URL for the private S3 object without exposing bucket access directly.
        return storageService.generateDownloadUrl(
                attachment.getS3Bucket(),
                attachment.getS3ObjectKey(),
                attachment.getOriginalFilename()
        );
    }

    private void validate(ChatAttachmentUpload upload) {
        if (upload == null || upload.inputStream() == null) {
            throw new InvalidChatAttachmentException("Attachment file is required");
        }

        if (upload.originalFilename() == null || upload.originalFilename().isBlank()) {
            throw new InvalidChatAttachmentException("Original filename is required");
        }

        if (upload.originalFilename().trim().length() > MAX_FILENAME_LENGTH) {
            throw new InvalidChatAttachmentException("Original filename is too long");
        }

        if (upload.contentType() == null || upload.contentType().isBlank()) {
            throw new InvalidChatAttachmentException("Content type is required");
        }

        if (upload.contentType().trim().length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new InvalidChatAttachmentException("Content type is too long");
        }

        if (upload.fileSizeBytes() <= 0) {
            throw new InvalidChatAttachmentException("File size must be greater than zero");
        }

        if (upload.fileSizeBytes() > storageProperties.getAttachments().getMaxFileSizeBytes()) {
            throw new InvalidChatAttachmentException("File size exceeds the maximum allowed attachment size");
        }

        if (!storageProperties.isAllowedAttachmentContentType(upload.contentType().trim())) {
            throw new InvalidChatAttachmentException("File type is not supported");
        }

        if (upload.caption() != null && upload.caption().trim().length() > MAX_CAPTION_LENGTH) {
            throw new InvalidChatAttachmentException("Caption cannot exceed 1000 characters");
        }
    }

    private String storageKeyPrefix(UUID roomId) {
        return "chat-attachments/chat-rooms/" + roomId;
    }
}