package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatRoomServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatRoomParticipantRepository participants = mock(ChatRoomParticipantRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);

    private final ChatRoomService service = new ChatRoomService(rooms, participants, messages, CLOCK);

    @Test
    void openRoomForAcceptedRequestCreatesRoomAndAddsPosterAndRequester() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        InvitePost post = invitePost(poster);

        when(rooms.findByInvitePost(post)).thenReturn(Optional.empty());
        when(rooms.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRoom room = service.openRoomForAcceptedRequest(post, requester);

        assertThat(room.getInvitePost()).isEqualTo(post);
        assertThat(room.hasParticipant(poster)).isTrue();
        assertThat(room.hasParticipant(requester)).isTrue();
        assertThat(room.getParticipants()).hasSize(2);

        verify(rooms).save(room);
    }

    @Test
    void openRoomForAcceptedRequestAddsRequesterToExistingGroupRoom() {
        AppUser poster = user("poster@example.com");
        AppUser firstRequester = user("first@example.com");
        AppUser secondRequester = user("second@example.com");
        InvitePost post = invitePost(poster);
        ChatRoom existingRoom = new ChatRoom(post, NOW.minusSeconds(300));
        existingRoom.addParticipant(poster, NOW.minusSeconds(300));
        existingRoom.addParticipant(firstRequester, NOW.minusSeconds(300));

        when(rooms.findByInvitePost(post)).thenReturn(Optional.of(existingRoom));
        when(rooms.save(existingRoom)).thenReturn(existingRoom);

        ChatRoom room = service.openRoomForAcceptedRequest(post, secondRequester);

        assertThat(room.hasParticipant(poster)).isTrue();
        assertThat(room.hasParticipant(firstRequester)).isTrue();
        assertThat(room.hasParticipant(secondRequester)).isTrue();
        assertThat(room.getParticipants()).hasSize(3);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);

        verify(rooms).save(existingRoom);
    }

    @Test
    void getRoomsForReturnsRoomsForParticipant() {
        AppUser user = user("member@example.com");
        List<ChatRoom> expectedRooms = List.of(new ChatRoom(invitePost(user("poster@example.com")), NOW));

        when(rooms.findDistinctByParticipantsUserOrderByUpdatedAtDesc(user)).thenReturn(expectedRooms);

        assertThat(service.getRoomsFor(user)).isEqualTo(expectedRooms);
    }

    @Test
    void getMessagesReturnsMessagesForParticipant() {
        AppUser sender = user("sender@example.com");
        ChatRoom room = new ChatRoom(invitePost(sender), NOW);
        room.addParticipant(sender, NOW);

        UUID roomId = UUID.randomUUID();
        ChatMessage message = new ChatMessage(room, sender, "Hello", NOW);

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUser(room, sender)).thenReturn(true);
        when(messages.findByChatRoomOrderByCreatedAtAscIdAsc(room)).thenReturn(List.of(message));

        assertThat(service.getMessages(sender, roomId)).containsExactly(message);
    }

    @Test
    void getMessagesRejectsNonParticipant() {
        AppUser sender = user("sender@example.com");
        AppUser outsider = user("outsider@example.com");
        ChatRoom room = new ChatRoom(invitePost(sender), NOW);

        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUser(room, outsider)).thenReturn(false);

        assertThatThrownBy(() -> service.getMessages(outsider, roomId))
                .isInstanceOf(ChatParticipantRequiredException.class);
    }

    @Test
    void sendMessageCreatesMessageForParticipant() {
        AppUser sender = user("sender@example.com");
        ChatRoom room = new ChatRoom(invitePost(sender), NOW);
        room.addParticipant(sender, NOW);

        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUser(room, sender)).thenReturn(true);
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage message = service.sendMessage(sender, roomId, "  Hello there  ");

        assertThat(message.getChatRoom()).isEqualTo(room);
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getBody()).isEqualTo("Hello there");
        assertThat(message.getCreatedAt()).isEqualTo(NOW);

        verify(messages).save(message);
    }

    @Test
    void sendMessageRejectsNonParticipant() {
        AppUser sender = user("sender@example.com");
        AppUser outsider = user("outsider@example.com");
        ChatRoom room = new ChatRoom(invitePost(sender), NOW);

        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUser(room, outsider)).thenReturn(false);

        assertThatThrownBy(() -> service.sendMessage(outsider, roomId, "Hello"))
                .isInstanceOf(ChatParticipantRequiredException.class);
    }

    @Test
    void sendMessageRejectsMissingRoom() {
        AppUser sender = user("sender@example.com");
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendMessage(sender, roomId, "Hello"))
                .isInstanceOf(ChatRoomNotFoundException.class);
    }

    private InvitePost invitePost(AppUser poster) {
        poster.markEmailVerified(NOW);
        poster.verifyLocation("San Francisco", "California", "USA", NOW);

        return new InvitePost(
                poster,
                "Anyone want coffee?",
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW
        );
    }

    private AppUser user(String email) {
        return new AppUser(
                "test_" + Math.abs(email.hashCode()),
                "Test",
                "User",
                email,
                "hashed-password",
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}