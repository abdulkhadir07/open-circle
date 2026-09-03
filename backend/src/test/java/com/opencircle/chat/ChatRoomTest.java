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

class ChatRoomTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void constructorCreatesRoomForInvitePost() {
        InvitePost post = invitePost(user("poster@example.com"));

        ChatRoom room = new ChatRoom(post, NOW);

        assertThat(room.getInvitePost()).isEqualTo(post);
        assertThat(room.getCreatedAt()).isEqualTo(NOW);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW);
        assertThat(room.getParticipants()).isEmpty();
    }

    @Test
    void addParticipantAddsUserToRoom() {
        ChatRoom room = new ChatRoom(invitePost(user("poster@example.com")), NOW);
        AppUser requester = user("requester@example.com");

        room.addParticipant(requester, NOW.plusSeconds(60));

        assertThat(room.hasParticipant(requester)).isTrue();
        assertThat(room.getParticipants()).hasSize(1);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void addParticipantDoesNotDuplicateSameUser() {
        ChatRoom room = new ChatRoom(invitePost(user("poster@example.com")), NOW);
        AppUser requester = user("requester@example.com");

        room.addParticipant(requester, NOW.plusSeconds(60));
        room.addParticipant(requester, NOW.plusSeconds(120));

        assertThat(room.getParticipants()).hasSize(1);
        assertThat(room.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void constructorRejectsMissingInvitePost() {
        assertThatThrownBy(() -> new ChatRoom(null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invite post is required");
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