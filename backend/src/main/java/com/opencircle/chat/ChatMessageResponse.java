package com.opencircle.chat;

import com.opencircle.profileimage.ProfileImageResponse;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        UUID senderId,
        String senderUsername,
        ProfileImageResponse senderProfileImage,
        ChatMessageType type,
        String body,
        AttachmentResponse attachment,
        Instant createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null);
    }

    public static ChatMessageResponse from(
            ChatMessage message,
            ProfileImageResponse senderProfileImage
    ) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                senderProfileImage,
                message.getType(),
                message.getBody(),
                AttachmentResponse.from(message.getAttachment()),
                message.getCreatedAt()
        );
    }

    public record AttachmentResponse(
            UUID id,
            String originalFilename,
            String contentType,
            long fileSizeBytes
    ) {

        private static AttachmentResponse from(ChatAttachment attachment) {
            if (attachment == null) {
                return null;
            }

            return new AttachmentResponse(
                    attachment.getId(),
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    attachment.getFileSizeBytes()
            );
        }
    }
}