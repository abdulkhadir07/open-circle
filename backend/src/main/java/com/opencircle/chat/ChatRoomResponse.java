package com.opencircle.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID invitePostId,
        String invitePostContent,
        List<ParticipantResponse> participants,
        Instant createdAt,
        Instant updatedAt
) {

    static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getInvitePost().getId(),
                room.getInvitePost().getContent(),
                room.getParticipants().stream()
                        .map(ParticipantResponse::from)
                        .toList(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public record ParticipantResponse(
            UUID userId,
            String username,
            Instant joinedAt
    ) {

        static ParticipantResponse from(ChatRoomParticipant participant) {
            return new ParticipantResponse(
                    participant.getUser().getId(),
                    participant.getUser().getUsername(),
                    participant.getJoinedAt()
            );
        }
    }
}