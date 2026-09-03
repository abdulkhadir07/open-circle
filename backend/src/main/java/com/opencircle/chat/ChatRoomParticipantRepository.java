package com.opencircle.chat;

import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, UUID> {

    boolean existsByChatRoomAndUser(ChatRoom chatRoom, AppUser user);

    Optional<ChatRoomParticipant> findByChatRoomAndUser(ChatRoom chatRoom, AppUser user);
}