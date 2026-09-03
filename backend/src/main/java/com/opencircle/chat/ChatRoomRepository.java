package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByInvitePost(InvitePost invitePost);

    @EntityGraph(attributePaths = {"invitePost", "participants", "participants.user"})
    List<ChatRoom> findDistinctByParticipantsUserOrderByUpdatedAtDesc(AppUser user);
}