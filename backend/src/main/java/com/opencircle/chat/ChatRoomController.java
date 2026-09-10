package com.opencircle.chat;

import com.opencircle.profileimage.ProfileImageQueryService;
import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.security.CurrentUserProvider;
import com.opencircle.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final CurrentUserProvider currentUserProvider;
    private final ChatRoomService chatRoomService;
    private final ChatAttachmentService chatAttachmentService;
    private final ChatMessageBroadcaster messageBroadcaster;
    private final ProfileImageQueryService profileImageQueryService;

    ChatRoomController(
            CurrentUserProvider currentUserProvider,
            ChatRoomService chatRoomService,
            ChatAttachmentService chatAttachmentService,
            ChatMessageBroadcaster messageBroadcaster,
            ProfileImageQueryService profileImageQueryService
    ) {
        this.currentUserProvider = currentUserProvider;
        this.chatRoomService = chatRoomService;
        this.chatAttachmentService = chatAttachmentService;
        this.messageBroadcaster = messageBroadcaster;
        this.profileImageQueryService = profileImageQueryService;
    }

    @GetMapping
    public List<ChatRoomResponse> getMyRooms(@AuthenticationPrincipal Jwt jwt) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Returns only rooms still visible to the authenticated participant.
        List<ChatRoom> rooms = chatRoomService.getRoomsFor(currentUser);
        Map<UUID, ProfileImageResponse> profileImagesByUser = profileImagesForRooms(rooms);

        return rooms.stream()
                .map(room -> ChatRoomResponse.from(room, currentUser, profileImagesByUser))
                .toList();
    }

    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponse> getMessages(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Reads room messages only after active membership is confirmed by the service.
        List<ChatMessage> messages = chatRoomService.getMessages(currentUser, roomId);
        Set<UUID> senderIds = messages.stream()
                .map(message -> message.getSender().getId())
                .collect(Collectors.toSet());
        Map<UUID, ProfileImageResponse> profileImagesByUser =
                profileImageQueryService.getProfileImagesByUserIds(senderIds);

        return messages.stream()
                .map(message -> ChatMessageResponse.from(
                        message,
                        profileImagesByUser.get(message.getSender().getId())
                ))
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

        // Creates a text message as the authenticated active participant.
        ChatMessage message = chatRoomService.sendMessage(currentUser, roomId, request.body());

        return responseFor(message);
    }

    @PostMapping(value = "/{roomId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse uploadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        try {
            ChatAttachmentUpload upload = new ChatAttachmentUpload(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    file.getInputStream(),
                    caption
            );

            // Stores the file, creates an attachment message, then broadcasts after the service transaction returns.
            ChatMessage message = chatAttachmentService.uploadAttachment(currentUser, roomId, upload);
            ChatMessageResponse response = responseFor(message);
            messageBroadcaster.broadcast(response);

            return response;
        } catch (IOException exception) {
            throw new InvalidChatAttachmentException("Unable to read uploaded file");
        }
    }

    @PatchMapping("/{roomId}/save")
    public ChatRoomResponse saveRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Saves the whole room so it cannot be auto-closed later.
        return responseFor(chatRoomService.saveRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/leave")
    public ChatRoomResponse leaveRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Marks the authenticated user inactive in the room and refreshes auto-close state.
        return responseFor(chatRoomService.leaveRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/hide")
    public ChatRoomResponse hideRoom(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Hides the room only from the authenticated user's room list.
        return responseFor(chatRoomService.hideRoom(currentUser, roomId), currentUser);
    }

    @PatchMapping("/{roomId}/participants/{userId}/remove")
    public ChatRoomResponse removeParticipant(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID roomId,
            @PathVariable UUID userId
    ) {
        AppUser currentUser = currentUserProvider.getCurrentUser(jwt);

        // Allows the poster to remove a participant from their invite-post chat room.
        return responseFor(chatRoomService.removeParticipant(currentUser, roomId, userId), currentUser);
    }

    private ChatMessageResponse responseFor(ChatMessage message) {
        return ChatMessageResponse.from(
                message,
                profileImageQueryService.getProfileImageByUserId(message.getSender().getId())
        );
    }

    private ChatRoomResponse responseFor(ChatRoom room, AppUser currentUser) {
        return ChatRoomResponse.from(room, currentUser, profileImagesForRooms(List.of(room)));
    }

    private Map<UUID, ProfileImageResponse> profileImagesForRooms(List<ChatRoom> rooms) {
        Set<UUID> userIds = new LinkedHashSet<>();

        rooms.forEach(room -> {
            addUserId(userIds, room.getSavedBy());
            room.getParticipants().forEach(participant -> {
                addUserId(userIds, participant.getUser());
                addUserId(userIds, participant.getRemovedBy());
            });
        });

        return profileImageQueryService.getProfileImagesByUserIds(userIds);
    }

    private void addUserId(Set<UUID> userIds, AppUser user) {
        if (user != null) {
            userIds.add(user.getId());
        }
    }
}
