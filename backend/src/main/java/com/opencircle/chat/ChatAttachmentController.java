package com.opencircle.chat;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
class ChatAttachmentController {

    private final CurrentUserProvider currentUserProvider;
    private final ChatAttachmentService chatAttachmentService;

    ChatAttachmentController(
            CurrentUserProvider currentUserProvider,
            ChatAttachmentService chatAttachmentService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.chatAttachmentService = chatAttachmentService;
    }

    @GetMapping("/{attachmentId}/download-url")
    AttachmentDownloadUrlResponse getDownloadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID attachmentId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Returns a temporary URL only after confirming the requester still belongs to the attachment's room.
        return AttachmentDownloadUrlResponse.from(
                chatAttachmentService.getDownloadUrl(currentUser, attachmentId)
        );
    }
}