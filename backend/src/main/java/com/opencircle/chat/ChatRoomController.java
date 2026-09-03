package com.opencircle.chat;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final CurrentUserProvider currentUserProvider;
    private final ChatRoomService chatRoomService;

    ChatRoomController(CurrentUserProvider currentUserProvider, ChatRoomService chatRoomService) {
        this.currentUserProvider = currentUserProvider;
        this.chatRoomService = chatRoomService;
    }

    @GetMapping
    public List<ChatRoomResponse> getMyRooms(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Returns only rooms where the authenticated user is a participant.
        return chatRoomService.getRoomsFor(currentUser).stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Reads room messages only after membership is verified by the service.
        return chatRoomService.getMessages(currentUser, roomId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @PostMapping("/{roomId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Creates a message as the authenticated participant.
        ChatMessage message = chatRoomService.sendMessage(currentUser, roomId, request.body());

        return ChatMessageResponse.from(message);
    }
}