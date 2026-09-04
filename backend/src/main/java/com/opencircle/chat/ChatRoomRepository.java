package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "invitePost",
            "invitePost.poster",
            "savedBy",
            "participants",
            "participants.user",
            "participants.removedBy"
    })
    Optional<ChatRoom> findById(UUID id);

    Optional<ChatRoom> findByInvitePost(InvitePost invitePost);

    @EntityGraph(attributePaths = {"invitePost", "participants", "participants.user"})
    List<ChatRoom> findDistinctByParticipantsUserOrderByUpdatedAtDesc(AppUser user);

    @EntityGraph(attributePaths = {
            "invitePost",
            "invitePost.poster",
            "savedBy",
            "participants",
            "participants.user",
            "participants.removedBy"
    })
    @Query("""
            SELECT DISTINCT room
            FROM ChatRoom room
            JOIN room.participants participant
            WHERE participant.user = :user
              AND participant.leftAt IS NULL
              AND participant.removedAt IS NULL
              AND participant.hiddenAt IS NULL
            ORDER BY room.updatedAt DESC
            """)
    List<ChatRoom> findVisibleRoomsFor(@Param("user") AppUser user);

    @Query("""
            SELECT room
            FROM ChatRoom room
            WHERE room.status = com.opencircle.chat.ChatRoomStatus.ACTIVE
              AND room.savedAt IS NULL
              AND room.autoCloseAt IS NOT NULL
              AND room.autoCloseAt <= :now
            """)
    List<ChatRoom> findRoomsReadyToAutoClose(@Param("now") Instant now);
}