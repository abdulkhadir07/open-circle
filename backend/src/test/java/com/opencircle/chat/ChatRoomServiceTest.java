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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        assertThat(room.getCreatedAt()).isEqualTo(NOW);

        verify(rooms).save(room);
    }

    @Test
    void openRoomForAcceptedRequestAddsRequesterToExistingGroupRoom() {
        AppUser poster = user("poster@example.com");
        AppUser firstRequester = user("first@example.com");
        AppUser secondRequester = user("second@example.com");
        InvitePost post = invitePost(poster);

        ChatRoom existingRoom = new ChatRoom(post, NOW.minusSeconds(60));
        existingRoom.addParticipant(poster, NOW.minusSeconds(60));
        existingRoom.addParticipant(firstRequester, NOW.minusSeconds(50));

        when(rooms.findByInvitePost(post)).thenReturn(Optional.of(existingRoom));
        when(rooms.save(existingRoom)).thenReturn(existingRoom);

        ChatRoom room = service.openRoomForAcceptedRequest(post, secondRequester);

        assertThat(room.hasParticipant(poster)).isTrue();
        assertThat(room.hasParticipant(firstRequester)).isTrue();
        assertThat(room.hasParticipant(secondRequester)).isTrue();

        verify(rooms).save(existingRoom);
    }

    @Test
    void getRoomsForReturnsVisibleRoomsForParticipant() {
        AppUser user = user("member@example.com");
        List<ChatRoom> expectedRooms = List.of(new ChatRoom(invitePost(user("poster@example.com")), NOW));

        when(rooms.findVisibleRoomsFor(user)).thenReturn(expectedRooms);

        assertThat(service.getRoomsFor(user)).isEqualTo(expectedRooms);
    }

    @Test
    void getMessagesReturnsMessagesForActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();
        List<ChatMessage> expectedMessages = List.of(new ChatMessage(room, poster, "Hello", NOW));

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);
        when(messages.findByChatRoomOrderByCreatedAtAscIdAsc(room)).thenReturn(expectedMessages);

        assertThat(service.getMessages(requester, roomId)).isEqualTo(expectedMessages);
    }

    @Test
    void getMessagesRejectsNonParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser outsider = user("outsider@example.com");
        ChatRoom room = roomWithParticipants(poster, user("requester@example.com"));
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, outsider)).thenReturn(false);

        assertThatThrownBy(() -> service.getMessages(outsider, roomId))
                .isInstanceOf(ChatParticipantRequiredException.class);
    }

    @Test
    void sendMessageCreatesMessageForActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);
        when(rooms.save(room)).thenReturn(room);
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessage message = service.sendMessage(requester, roomId, "  Hello there  ");

        assertThat(message.getChatRoom()).isEqualTo(room);
        assertThat(message.getSender()).isEqualTo(requester);
        assertThat(message.getBody()).isEqualTo("Hello there");
        assertThat(message.getCreatedAt()).isEqualTo(NOW);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);

        verify(rooms).save(room);
        verify(messages).save(message);
    }

    @Test
    void sendMessageRejectsNonParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser outsider = user("outsider@example.com");
        ChatRoom room = roomWithParticipants(poster, user("requester@example.com"));
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, outsider)).thenReturn(false);

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

    @Test
    void sendMessageRejectsClosedRoom() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        room.close(NOW.minusSeconds(10));

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);

        assertThatThrownBy(() -> service.sendMessage(requester, roomId, "Hello"))
                .isInstanceOf(ChatRoomActionNotAllowedException.class);
    }

    @Test
    void sendMessageRejectsRoomWithOnlyOneActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        room.leave(requester, NOW.minusSeconds(30));

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, poster)).thenReturn(true);

        assertThatThrownBy(() -> service.sendMessage(poster, roomId, "Anyone here?"))
                .isInstanceOf(ChatRoomActionNotAllowedException.class);
    }

    @Test
    void isActiveParticipantReturnsTrueWhenUserIsActiveParticipant() {
        AppUser user = user("participant@example.com");
        ChatRoom room = roomWithParticipants(user, user("other.participant@example.com"));
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, user))
                .thenReturn(true);

        assertThat(service.isActiveParticipant(user, roomId)).isTrue();
    }

    @Test
    void isActiveParticipantReturnsFalseWhenUserIsNotActiveParticipant() {
        AppUser user = user("inactive.participant@example.com");
        ChatRoom room = roomWithParticipants(user("poster.active-check@example.com"), user("member.active-check@example.com"));
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, user))
                .thenReturn(false);

        assertThat(service.isActiveParticipant(user, roomId)).isFalse();
    }

    @Test
    void isActiveParticipantReturnsFalseWhenRoomDoesNotExist() {
        AppUser user = user("participant.missing@example.com");
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.empty());

        assertThat(service.isActiveParticipant(user, roomId)).isFalse();
    }

    @Test
    void saveRoomMarksRoomSavedByActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);
        when(rooms.save(room)).thenReturn(room);

        ChatRoom savedRoom = service.saveRoom(requester, roomId);

        assertThat(savedRoom.isSaved()).isTrue();
        assertThat(savedRoom.getSavedAt()).isEqualTo(NOW);
        assertThat(savedRoom.getSavedBy()).isEqualTo(requester);

        verify(rooms).save(room);
    }

    @Test
    void leaveRoomMarksParticipantInactiveAndStartsAutoCloseCountdown() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);
        when(rooms.save(room)).thenReturn(room);

        ChatRoom updatedRoom = service.leaveRoom(requester, roomId);

        assertThat(updatedRoom.hasActiveParticipant(requester)).isFalse();
        assertThat(updatedRoom.getAutoCloseAt()).isEqualTo(NOW.plusSeconds(24 * 60 * 60));

        verify(rooms).save(room);
    }

    @Test
    void removeParticipantMarksRequesterRemovedWhenCurrentUserIsPoster() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        ChatRoomParticipant requesterParticipant = participantFor(room, requester);

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, poster)).thenReturn(true);
        when(participants.findByChatRoomAndUser_Id(room, requesterId)).thenReturn(Optional.of(requesterParticipant));
        when(rooms.save(room)).thenReturn(room);

        ChatRoom updatedRoom = service.removeParticipant(poster, roomId, requesterId);

        assertThat(updatedRoom.hasActiveParticipant(requester)).isFalse();
        assertThat(requesterParticipant.getRemovedAt()).isEqualTo(NOW);
        assertThat(requesterParticipant.getRemovedBy()).isEqualTo(poster);

        verify(rooms).save(room);
    }

    @Test
    void removeParticipantRejectsNonPoster() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);

        assertThatThrownBy(() -> service.removeParticipant(requester, roomId, UUID.randomUUID()))
                .isInstanceOf(ChatRoomForbiddenException.class);
    }

    @Test
    void hideRoomMarksParticipantHiddenWithoutRemovingAccess() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = roomWithParticipants(poster, requester);
        UUID roomId = UUID.randomUUID();
        ChatRoomParticipant requesterParticipant = participantFor(room, requester);

        when(rooms.findById(roomId)).thenReturn(Optional.of(room));
        when(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).thenReturn(true);
        when(rooms.save(room)).thenReturn(room);

        ChatRoom updatedRoom = service.hideRoom(requester, roomId);

        assertThat(updatedRoom.hasActiveParticipant(requester)).isTrue();
        assertThat(requesterParticipant.getHiddenAt()).isEqualTo(NOW);

        verify(rooms).save(room);
    }

    @Test
    void closeRoomsReadyForAutoCloseClosesReadyRooms() {
        ChatRoom room = roomWithParticipants(user("poster@example.com"), user("requester@example.com"));

        when(rooms.findRoomsReadyToAutoClose(NOW)).thenReturn(List.of(room));

        int closedCount = service.closeRoomsReadyForAutoClose();

        assertThat(closedCount).isEqualTo(1);
        assertThat(room.isClosed()).isTrue();
        assertThat(room.getClosedAt()).isEqualTo(NOW);

        verify(rooms).saveAll(List.of(room));
    }

    private ChatRoom roomWithParticipants(AppUser poster, AppUser requester) {
        ChatRoom room = new ChatRoom(invitePost(poster), NOW.minusSeconds(120));
        room.addParticipant(poster, NOW.minusSeconds(120));
        room.addParticipant(requester, NOW.minusSeconds(90));
        return room;
    }

    private ChatRoomParticipant participantFor(ChatRoom room, AppUser user) {
        return room.getParticipants().stream()
                .filter(participant -> participant.belongsTo(user))
                .findFirst()
                .orElseThrow();
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
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
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
