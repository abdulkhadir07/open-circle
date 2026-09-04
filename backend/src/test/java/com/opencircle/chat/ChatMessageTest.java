package com.opencircle.chat;

import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatMessageTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void constructorCreatesMessageFromParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(30));

        ChatMessage message = new ChatMessage(room, poster, "  Hey there!  ", NOW.plusSeconds(60));

        assertThat(message.getChatRoom()).isEqualTo(room);
        assertThat(message.getSender()).isEqualTo(poster);
        assertThat(message.getBody()).isEqualTo("Hey there!");
        assertThat(message.getCreatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void constructorRejectsBlankBody() {
        AppUser poster = user("poster@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);

        assertThatThrownBy(() -> new ChatMessage(room, poster, "   ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message body is required");
    }

    @Test
    void constructorRejectsSenderWhoIsNotParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser stranger = user("stranger@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);

        assertThatThrownBy(() -> new ChatMessage(room, stranger, "Can I join?", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sender must be an active chat room participant");
    }

    @Test
    void constructorRejectsSenderWhoLeftRoom() {
        AppUser poster = user("poster@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.leave(poster, NOW.plusSeconds(60));

        assertThatThrownBy(() -> new ChatMessage(room, poster, "Still here?", NOW.plusSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sender must be an active chat room participant");
    }

    @Test
    void constructorRejectsClosedRoom() {
        AppUser poster = user("poster@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.close(NOW.plusSeconds(60));

        assertThatThrownBy(() -> new ChatMessage(room, poster, "Anyone here?", NOW.plusSeconds(120)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Closed chat rooms cannot receive new messages");
    }

    @Test
    void constructorRejectsRoomWithOnlyOneActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.leave(requester, NOW.plusSeconds(120));

        assertThatThrownBy(() -> new ChatMessage(room, poster, "Still here?", NOW.plusSeconds(180)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least two active participants are required to send messages");
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