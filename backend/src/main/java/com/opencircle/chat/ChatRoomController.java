package com.opencircle.chat;

import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

        // Returns only rooms still visible to the authenticated participant.
        return chatRoomService.getRoomsFor(currentUser).stream()
                .map(room -> ChatRoomResponse.from(room, currentUser))
                .toList();
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Reads room messages only after active membership is confirmed by the service.
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

        // Creates a message as the authenticated active participant.
        ChatMessage message = chatRoomService.sendMessage(currentUser, roomId, request.body());

        return ChatMessageResponse.from(message);
    }

    @PatchMapping("/{roomId}/save")
    public ChatRoomResponse saveRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Saves the whole room so it cannot be auto-closed later.
        return ChatRoomResponse.from(chatRoomService.saveRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/leave")
    public ChatRoomResponse leaveRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Marks the authenticated user inactive in the room and refreshes auto-close state.
        return ChatRoomResponse.from(chatRoomService.leaveRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/hide")
    public ChatRoomResponse hideRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Hides the room only from the authenticated user's room list.
        return ChatRoomResponse.from(chatRoomService.hideRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/participants/{userId}/remove")
    public ChatRoomResponse removeParticipant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @PathVariable UUID userId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Allows the poster to remove a participant from their invite-post chat room.
        return ChatRoomResponse.from(chatRoomService.removeParticipant(currentUser, roomId, userId), currentUser);
    }
}