package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

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
    void participantRepositoryDetectsMembership() {
        AppUser poster = verifiedUser("poster.participant@example.com");
        AppUser requester = verifiedUser("requester.participant@example.com");
        ChatRoom room = rooms.save(new ChatRoom(posts.save(invitePost(poster)), NOW));

        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW);
        rooms.save(room);

        assertThat(participants.existsByChatRoomAndUser(room, requester)).isTrue();
        assertThat(participants.findByChatRoomAndUser(room, requester)).isPresent();
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