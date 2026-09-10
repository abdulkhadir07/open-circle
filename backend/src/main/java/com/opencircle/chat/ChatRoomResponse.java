package com.opencircle.chat;

import com.opencircle.profileimage.ProfileImageResponse;
import com.opencircle.user.AppUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID invitePostId,
        String invitePostContent,
        ChatRoomStatus status,
        boolean saved,
        Instant savedAt,
        UUID savedByUserId,
        String savedByUsername,
        ProfileImageResponse savedByProfileImage,
        Instant autoCloseAt,
        boolean closed,
        Instant closedAt,
        boolean hiddenForCurrentUser,
        List<ParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChatRoomResponse from(ChatRoom room, AppUser currentUser) {
        return from(room, currentUser, Map.of());
    }

    public static ChatRoomResponse from(
            ChatRoom room,
            AppUser currentUser,
            Map<UUID, ProfileImageResponse> profileImagesByUser
    ) {
        return new ChatRoomResponse(
                room.getId(),
                room.getInvitePost().getId(),
                room.getInvitePost().getContent(),
                room.getStatus(),
                room.isSaved(),
                room.getSavedAt(),
                nullableUserId(room.getSavedBy()),
                nullableUsername(room.getSavedBy()),
                profileImageFor(room.getSavedBy(), profileImagesByUser),
                room.getAutoCloseAt(),
                room.isClosed(),
                room.getClosedAt(),
                hiddenForCurrentUser(room, currentUser),
                room.getParticipants().stream()
                        .map(participant -> ParticipantResponse.from(participant, profileImagesByUser))
                        .toList(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public static ChatRoomResponse from(ChatRoom room) {
        return from(room, null, Map.of());
    }

    private static boolean hiddenForCurrentUser(ChatRoom room, AppUser currentUser) {
        if (currentUser == null) {
            return false;
        }

        return room.getParticipants().stream()
                .filter(participant -> participant.belongsTo(currentUser))
                .findFirst()
                .map(participant -> participant.getHiddenAt() != null)
                .orElse(false);
    }

    private static UUID nullableUserId(AppUser user) {
        return user == null ? null : user.getId();
    }

    private static String nullableUsername(AppUser user) {
        return user == null ? null : user.getUsername();
    }

    private static ProfileImageResponse profileImageFor(
            AppUser user,
            Map<UUID, ProfileImageResponse> profileImagesByUser
    ) {
        return user == null ? null : profileImagesByUser.get(user.getId());
    }

    public record ParticipantResponse(
            UUID userId,
            String username,
            ProfileImageResponse profileImage,
            boolean active,
            Instant joinedAt,
            boolean left,
            Instant leftAt,
            boolean removed,
            Instant removedAt,
            UUID removedByUserId,
            String removedByUsername,
            ProfileImageResponse removedByProfileImage
    ) {

        static ParticipantResponse from(
                ChatRoomParticipant participant,
                Map<UUID, ProfileImageResponse> profileImagesByUser
        ) {
            return new ParticipantResponse(
                    participant.getUser().getId(),
                    participant.getUser().getUsername(),
                    ChatRoomResponse.profileImageFor(participant.getUser(), profileImagesByUser),
                    participant.isActive(),
                    participant.getJoinedAt(),
                    participant.getLeftAt() != null,
                    participant.getLeftAt(),
                    participant.getRemovedAt() != null,
                    participant.getRemovedAt(),
                    ChatRoomResponse.nullableUserId(participant.getRemovedBy()),
                    ChatRoomResponse.nullableUsername(participant.getRemovedBy()),
                    ChatRoomResponse.profileImageFor(participant.getRemovedBy(), profileImagesByUser)
            );
        }
    }
}
