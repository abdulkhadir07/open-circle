package com.opencircle.chat;

import com.opencircle.user.AppUser;

import java.time.Instant;
import java.util.List;
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
        Instant autoCloseAt,
        boolean closed,
        Instant closedAt,
        boolean hiddenForCurrentUser,
        List<ParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChatRoomResponse from(ChatRoom room, AppUser currentUser) {
        return new ChatRoomResponse(
                room.getId(),
                room.getInvitePost().getId(),
                room.getInvitePost().getContent(),
                room.getStatus(),
                room.isSaved(),
                room.getSavedAt(),
                nullableUserId(room.getSavedBy()),
                nullableUsername(room.getSavedBy()),
                room.getAutoCloseAt(),
                room.isClosed(),
                room.getClosedAt(),
                hiddenForCurrentUser(room, currentUser),
                room.getParticipants().stream()
                        .map(ParticipantResponse::from)
                        .toList(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public static ChatRoomResponse from(ChatRoom room) {
        return from(room, null);
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

    public record ParticipantResponse(
            UUID userId,
            String username,
            boolean active,
            Instant joinedAt,
            boolean left,
            Instant leftAt,
            boolean removed,
            Instant removedAt,
            UUID removedByUserId,
            String removedByUsername
    ) {

        static ParticipantResponse from(ChatRoomParticipant participant) {
            return new ParticipantResponse(
                    participant.getUser().getId(),
                    participant.getUser().getUsername(),
                    participant.isActive(),
                    participant.getJoinedAt(),
                    participant.getLeftAt() != null,
                    participant.getLeftAt(),
                    participant.getRemovedAt() != null,
                    participant.getRemovedAt(),
                    ChatRoomResponse.nullableUserId(participant.getRemovedBy()),
                    ChatRoomResponse.nullableUsername(participant.getRemovedBy())
            );
        }
    }
}