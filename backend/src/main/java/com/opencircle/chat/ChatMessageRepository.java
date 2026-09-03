package com.opencircle.chat;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    @EntityGraph(attributePaths = {"chatRoom", "sender"})
    List<ChatMessage> findByChatRoomOrderByCreatedAtAscIdAsc(ChatRoom chatRoom);
}