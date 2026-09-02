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
        return rooms.findDistinctByParticipantsUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(AppUser viewer, UUID roomId) {
        ChatRoom room = getParticipantRoom(viewer, roomId);

        return messages.findByChatRoomOrderByCreatedAtAscIdAsc(room);
    }

    @Transactional
    public ChatMessage sendMessage(AppUser sender, UUID roomId, String body) {
        ChatRoom room = getParticipantRoom(sender, roomId);

        // Message construction enforces that the sender belongs to the room.
        ChatMessage message = new ChatMessage(room, sender, body, Instant.now(clock));

        return messages.save(message);
    }

    private ChatRoom getParticipantRoom(AppUser user, UUID roomId) {
        ChatRoom room = rooms.findById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);

        if (!participants.existsByChatRoomAndUser(room, user)) {
            throw new ChatParticipantRequiredException();
        }

        return room;
    }
}