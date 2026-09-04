package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatRoomRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Autowired
    private UserService users;

    @Autowired
    private InvitePostRepository posts;

    @Autowired
    private ChatRoomRepository rooms;

    @Autowired
    private ChatRoomParticipantRepository participants;

    @Autowired
    private ChatMessageRepository messages;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void findByInvitePostReturnsMatchingRoom() {
        AppUser poster = verifiedUser("poster.room@example.com");
        InvitePost post = posts.save(invitePost(poster));
        ChatRoom room = rooms.save(new ChatRoom(post, NOW));

        assertThat(rooms.findByInvitePost(post))
                .map(ChatRoom::getId)
                .contains(room.getId());
    }

    @Test
    void findRoomsForParticipantReturnsOnlyTheirRoomsNewestFirst() {
        AppUser poster = verifiedUser("poster.rooms@example.com");
        AppUser firstRequester = verifiedUser("first.requester.rooms@example.com");
        AppUser secondRequester = verifiedUser("second.requester.rooms@example.com");
        AppUser outsider = verifiedUser("outsider.rooms@example.com");

        ChatRoom olderRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster)), NOW.minusSeconds(120)));
        olderRoom.addParticipant(poster, NOW.minusSeconds(120));
        olderRoom.addParticipant(firstRequester, NOW.minusSeconds(120));

        ChatRoom newerRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Later coffee?")), NOW.minusSeconds(60)));
        newerRoom.addParticipant(poster, NOW.minusSeconds(60));
        newerRoom.addParticipant(firstRequester, NOW.minusSeconds(60));
        newerRoom.addParticipant(secondRequester, NOW.minusSeconds(60));

        ChatRoom outsiderRoom = rooms.save(new ChatRoom(posts.save(invitePost(outsider, "Outside room")), NOW.minusSeconds(30)));
        outsiderRoom.addParticipant(outsider, NOW.minusSeconds(30));

        rooms.save(olderRoom);
        rooms.save(newerRoom);
        rooms.save(outsiderRoom);

        assertThat(rooms.findDistinctByParticipantsUserOrderByUpdatedAtDesc(firstRequester))
                .extracting(ChatRoom::getId)
                .containsExactly(newerRoom.getId(), olderRoom.getId());
    }

    @Test
    void findVisibleRoomsForExcludesHiddenLeftAndRemovedRooms() {
        AppUser poster = verifiedUser("poster.visible@example.com");
        AppUser requester = verifiedUser("requester.visible@example.com");

        ChatRoom visibleRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Visible room")), NOW.minusSeconds(240)));
        visibleRoom.addParticipant(poster, NOW.minusSeconds(240));
        visibleRoom.addParticipant(requester, NOW.minusSeconds(240));

        ChatRoom hiddenRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Hidden room")), NOW.minusSeconds(180)));
        hiddenRoom.addParticipant(poster, NOW.minusSeconds(180));
        hiddenRoom.addParticipant(requester, NOW.minusSeconds(180));
        hiddenRoom.hideFor(requester, NOW.minusSeconds(170));

        ChatRoom leftRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Left room")), NOW.minusSeconds(120)));
        leftRoom.addParticipant(poster, NOW.minusSeconds(120));
        leftRoom.addParticipant(requester, NOW.minusSeconds(120));
        leftRoom.leave(requester, NOW.minusSeconds(110));

        ChatRoom removedRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Removed room")), NOW.minusSeconds(60)));
        removedRoom.addParticipant(poster, NOW.minusSeconds(60));
        removedRoom.addParticipant(requester, NOW.minusSeconds(60));
        removedRoom.removeParticipant(requester, poster, NOW.minusSeconds(50));

        rooms.save(visibleRoom);
        rooms.save(hiddenRoom);
        rooms.save(leftRoom);
        rooms.save(removedRoom);

        assertThat(rooms.findVisibleRoomsFor(requester))
                .extracting(ChatRoom::getId)
                .containsExactly(visibleRoom.getId());
    }

    @Test
    void participantRepositoryDetectsActiveMembershipWithoutTreatingHiddenAsInactive() {
        AppUser poster = verifiedUser("poster.participant@example.com");
        AppUser requester = verifiedUser("requester.participant@example.com");
        ChatRoom room = rooms.save(new ChatRoom(posts.save(invitePost(poster)), NOW));

        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW);
        room.hideFor(requester, NOW.plusSeconds(60));
        rooms.save(room);

        assertThat(participants.existsByChatRoomAndUser(room, requester)).isTrue();
        assertThat(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, requester)).isTrue();
        assertThat(participants.findByChatRoomAndUser(room, requester)).isPresent();
    }

    @Test
    void participantRepositoryDoesNotTreatLeftOrRemovedUsersAsActive() {
        AppUser poster = verifiedUser("poster.inactive@example.com");
        AppUser leftRequester = verifiedUser("left.requester@example.com");
        AppUser removedRequester = verifiedUser("removed.requester@example.com");
        ChatRoom room = rooms.save(new ChatRoom(posts.save(invitePost(poster)), NOW));

        room.addParticipant(poster, NOW);
        room.addParticipant(leftRequester, NOW);
        room.addParticipant(removedRequester, NOW);
        room.leave(leftRequester, NOW.plusSeconds(60));
        room.removeParticipant(removedRequester, poster, NOW.plusSeconds(120));
        rooms.save(room);

        assertThat(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, leftRequester)).isFalse();
        assertThat(participants.existsByChatRoomAndUserAndLeftAtIsNullAndRemovedAtIsNull(room, removedRequester)).isFalse();
    }

    @Test
    void findRoomsReadyToAutoCloseReturnsOnlyActiveUnsavedRoomsPastDeadline() {
        AppUser poster = verifiedUser("poster.autoclose@example.com");
        AppUser requester = verifiedUser("requester.autoclose@example.com");

        Instant oldJoinedAt = NOW.minusSeconds(24 * 60 * 60 + 600);
        Instant oldLeftAt = NOW.minusSeconds(24 * 60 * 60 + 60);

        ChatRoom readyRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Ready room")), oldJoinedAt));
        readyRoom.addParticipant(poster, oldJoinedAt);
        readyRoom.addParticipant(requester, oldJoinedAt.plusSeconds(60));
        readyRoom.leave(requester, oldLeftAt);

        ChatRoom savedRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Saved room")), oldJoinedAt));
        savedRoom.addParticipant(poster, oldJoinedAt);
        savedRoom.addParticipant(requester, oldJoinedAt.plusSeconds(60));
        savedRoom.leave(requester, oldLeftAt);
        savedRoom.save(poster, NOW.minusSeconds(30));

        ChatRoom futureRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Future room")), NOW.minusSeconds(600)));
        futureRoom.addParticipant(poster, NOW.minusSeconds(600));
        futureRoom.addParticipant(requester, NOW.minusSeconds(540));
        futureRoom.leave(requester, NOW.minusSeconds(60));

        ChatRoom closedRoom = rooms.save(new ChatRoom(posts.save(invitePost(poster, "Closed room")), oldJoinedAt));
        closedRoom.addParticipant(poster, oldJoinedAt);
        closedRoom.addParticipant(requester, oldJoinedAt.plusSeconds(60));
        closedRoom.leave(requester, oldLeftAt);
        closedRoom.close(NOW.minusSeconds(30));

        rooms.save(readyRoom);
        rooms.save(savedRoom);
        rooms.save(futureRoom);
        rooms.save(closedRoom);

        assertThat(rooms.findRoomsReadyToAutoClose(NOW))
                .extracting(ChatRoom::getId)
                .containsExactly(readyRoom.getId());
    }

    @Test
    void messagesReturnOldestFirst() {
        AppUser poster = verifiedUser("poster.messages@example.com");
        AppUser requester = verifiedUser("requester.messages@example.com");

        ChatRoom room = rooms.save(new ChatRoom(posts.save(invitePost(poster)), NOW));
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW);
        rooms.save(room);

        ChatMessage first = messages.save(new ChatMessage(room, poster, "First message", NOW.plusSeconds(10)));
        ChatMessage second = messages.save(new ChatMessage(room, requester, "Second message", NOW.plusSeconds(20)));

        assertThat(messages.findByChatRoomOrderByCreatedAtAscIdAsc(room))
                .extracting(ChatMessage::getId)
                .containsExactly(first.getId(), second.getId());
    }

    private InvitePost invitePost(AppUser poster) {
        return invitePost(poster, "Anyone want coffee?");
    }

    private InvitePost invitePost(AppUser poster, String content) {
        return new InvitePost(
                poster,
                content,
                InviteType.GROUP,
                3,
                LocationScope.CITY,
                poster.getVerifiedCity(),
                poster.getVerifiedStateRegion(),
                poster.getVerifiedCountry(),
                NOW
        );
    }

    private AppUser verifiedUser(String email) {
        AppUser user = users.createUser(
                "Test",
                "User",
                email,
                passwordEncoder.encode("Password123!"),
                "+1415555" + Math.abs(email.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        user.markEmailVerified(NOW);
        user.verifyLocation("San Francisco", "California", "USA", NOW);

        return user;
    }
}