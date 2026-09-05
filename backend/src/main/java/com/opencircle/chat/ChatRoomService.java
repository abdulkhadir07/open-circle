package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.user.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatRoomService {

    private final ChatRoomRepository rooms;
    private final ChatRoomParticipantRepository participants;
    private final ChatMessageRepository messages;
    private final Clock clock;

    ChatRoomService(
            ChatRoomRepository rooms,
            ChatRoomParticipantRepository participants,
            ChatMessageRepository messages,
            Clock clock
    ) {
        this.rooms = rooms;
        this.participants = participants;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional
    public ChatRoom openRoomForAcceptedRequest(InvitePost post, AppUser requester) {
        Instant now = Instant.now(clock);

        ChatRoom room = rooms.findByInvitePost(post)
                .orElseGet(() -> new ChatRoom(post, now));

        // Accepted engagement requests grant room access to both the poster and requester.
        room.addParticipant(post.getPoster(), now);
        room.addParticipant(requester, now);

        return rooms.save(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getRoomsFor(AppUser user) {
        return rooms.findVisibleRoomsFor(user);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(AppUser viewer, UUID roomId) {
        ChatRoom room = getActiveParticipantRoom(viewer, roomId);

        return messages.findByChatRoomOrderByCreatedAtAscIdAsc(room);
    }

    // Checks whether the user is still an active participant in the room.
    @Transactional(readOnly = true)
    public boolean isActiveParticipant(AppUser user, UUID roomId) {
        return rooms.findById(roomId)
                .map(room -> participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, user))
                .orElse(false);
    }

    @Transactional
    public ChatMessage sendMessage(AppUser sender, UUID roomId, String body) {
        ChatRoom room = getActiveParticipantRoom(sender, roomId);
        Instant now = Instant.now(clock);

        reconcileAutoClose(room, now);
        requireRoomCanReceiveMessages(room);

        ChatMessage message = new ChatMessage(room, sender, body, now);

        room.recordMessageSent(now);
        rooms.save(room);

        return messages.save(message);
    }

    @Transactional
    public ChatRoom saveRoom(AppUser user, UUID roomId) {
        ChatRoom room = getActiveParticipantRoom(user, roomId);
        Instant now = Instant.now(clock);

        reconcileAutoClose(room, now);

        return saveAfterAction(room, () -> room.save(user, now));
    }

    @Transactional
    public ChatRoom leaveRoom(AppUser user, UUID roomId) {
        ChatRoom room = getActiveParticipantRoom(user, roomId);
        Instant now = Instant.now(clock);

        reconcileAutoClose(room, now);

        if (room.isClosed()) {
            throw new ChatRoomActionNotAllowedException("Closed chat rooms cannot be left");
        }

        return saveAfterAction(room, () -> room.leave(user, now));
    }

    @Transactional
    public ChatRoom removeParticipant(AppUser poster, UUID roomId, UUID userId) {
        ChatRoom room = getActiveParticipantRoom(poster, roomId);
        requirePostOwner(room, poster);

        Instant now = Instant.now(clock);

        reconcileAutoClose(room, now);

        if (room.isClosed()) {
            throw new ChatRoomActionNotAllowedException("Closed chat rooms cannot have participants removed");
        }

        ChatRoomParticipant participant = participants.findByChatRoomAndUser_Id(room, userId)
                .orElseThrow(ChatParticipantRequiredException::new);

        return saveAfterAction(room, () -> room.removeParticipant(participant.getUser(), poster, now));
    }

    @Transactional
    public ChatRoom hideRoom(AppUser user, UUID roomId) {
        ChatRoom room = getActiveParticipantRoom(user, roomId);
        Instant now = Instant.now(clock);

        return saveAfterAction(room, () -> room.hideFor(user, now));
    }

    @Transactional
    public int closeRoomsReadyForAutoClose() {
        Instant now = Instant.now(clock);
        List<ChatRoom> readyRooms = rooms.findRoomsReadyToAutoClose(now);

        readyRooms.forEach(room -> room.close(now));
        rooms.saveAll(readyRooms);

        return readyRooms.size();
    }

    private ChatRoom getActiveParticipantRoom(AppUser user, UUID roomId) {
        ChatRoom room = rooms.findById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        if (!participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, user)) {
            throw new ChatParticipantRequiredException();
        }

        return room;
    }

    private void requireRoomCanReceiveMessages(ChatRoom room) {
        if (room.isClosed()) {
            throw new ChatRoomActionNotAllowedException("Closed chat rooms cannot receive new messages");
        }

        if (room.activeParticipantCount() < 2) {
            throw new ChatRoomActionNotAllowedException("At least two active participants are required to send messages");
        }
    }

    private void requirePostOwner(ChatRoom room, AppUser user) {
        if (!sameUser(room.getInvitePost().getPoster(), user)) {
            throw new ChatRoomForbiddenException("Only the poster can remove chat room participants");
        }
    }

    private ChatRoom saveAfterAction(ChatRoom room, Runnable action) {
        try {
            action.run();
            return rooms.save(room);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ChatRoomActionNotAllowedException(exception.getMessage());
        }
    }

    private boolean sameUser(AppUser first, AppUser second) {
        if (first == second) {
            return true;
        }

        return first != null
                && second != null
                && first.getId() != null
                && second.getId() != null
                && first.getId().equals(second.getId());
    }

    // Closes rooms whose auto-close deadline has passed before allowing new write actions.
    private void reconcileAutoClose(ChatRoom room, Instant now) {
        if (room.shouldAutoClose(now)) {
            room.close(now);
        }
    }
}