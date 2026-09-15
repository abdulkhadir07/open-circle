package com.opencircle.chat;

import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, UUID> {

    boolean existsByChatRoomAndUser(ChatRoom chatRoom, AppUser user);

    boolean existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(
            ChatRoom chatRoom,
            AppUser user
    );

    Optional<ChatRoomParticipant> findByChatRoomAndUser(ChatRoom chatRoom, AppUser user);

    Optional<ChatRoomParticipant> findByChatRoomAndUser_Id(ChatRoom chatRoom, UUID userId);

    @Query("""
            SELECT participant.user.id
            FROM ChatRoomParticipant participant
            WHERE participant.chatRoom = :chatRoom
              AND participant.user.id <> :senderUserId
              AND participant.leftAt IS NULL
              AND participant.removedAt IS NULL
              AND participant.hiddenAt IS NULL
            ORDER BY participant.user.id
            """)
    List<UUID> findChatActivityRecipientIds(
            @Param("chatRoom") ChatRoom chatRoom,
            @Param("senderUserId") UUID senderUserId
    );
}