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

class ChatRoomParticipantTest {

    private static final Instant NOW = Instant.parse("2026-09-01T12:00:00Z");

    @Test
    void constructorCreatesActiveParticipant() {
        ChatRoom room = room();
        AppUser user = user("participant@example.com");

        ChatRoomParticipant participant = new ChatRoomParticipant(room, user, NOW);

        assertThat(participant.getChatRoom()).isEqualTo(room);
        assertThat(participant.getUser()).isEqualTo(user);
        assertThat(participant.getJoinedAt()).isEqualTo(NOW);
        assertThat(participant.getLeftAt()).isNull();
        assertThat(participant.getRemovedAt()).isNull();
        assertThat(participant.getRemovedBy()).isNull();
        assertThat(participant.getHiddenAt()).isNull();
        assertThat(participant.isActive()).isTrue();
    }

    @Test
    void leaveMarksParticipantInactive() {
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);

        participant.leave(NOW.plusSeconds(60));

        assertThat(participant.getLeftAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(participant.isActive()).isFalse();
    }

    @Test
    void leaveRejectsInactiveParticipant() {
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);
        participant.leave(NOW.plusSeconds(60));

        assertThatThrownBy(() -> participant.leave(NOW.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active participants can leave the chat room");
    }

    @Test
    void removeMarksParticipantInactiveAndStoresRemover() {
        AppUser remover = user("poster@example.com");
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);

        participant.remove(remover, NOW.plusSeconds(60));

        assertThat(participant.getRemovedBy()).isEqualTo(remover);
        assertThat(participant.getRemovedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(participant.isActive()).isFalse();
    }

    @Test
    void removeRejectsInactiveParticipant() {
        AppUser remover = user("poster@example.com");
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);
        participant.remove(remover, NOW.plusSeconds(60));

        assertThatThrownBy(() -> participant.remove(remover, NOW.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active participants can be removed from the chat room");
    }

    @Test
    void removeRejectsMissingRemovedByUser() {
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);

        assertThatThrownBy(() -> participant.remove(null, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Removed by user is required");
    }

    @Test
    void hideStoresHiddenTime() {
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);

        participant.hide(NOW.plusSeconds(60));

        assertThat(participant.getHiddenAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void hideDoesNotOverwriteExistingHiddenTime() {
        ChatRoomParticipant participant = new ChatRoomParticipant(room(), user("participant@example.com"), NOW);

        participant.hide(NOW.plusSeconds(60));
        participant.hide(NOW.plusSeconds(120));

        assertThat(participant.getHiddenAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void constructorRejectsMissingChatRoom() {
        assertThatThrownBy(() -> new ChatRoomParticipant(null, user("participant@example.com"), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chat room is required");
    }

    @Test
    void constructorRejectsMissingUser() {
        assertThatThrownBy(() -> new ChatRoomParticipant(room(), null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User is required");
    }

    @Test
    void constructorRejectsMissingJoinedTime() {
        assertThatThrownBy(() -> new ChatRoomParticipant(room(), user("participant@example.com"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Joined time is required");
    }

    private ChatRoom room() {
        AppUser poster = user("poster@example.com");
        poster.markEmailVerified(NOW);
        poster.verifyLocation("San Francisco", "California", "USA", NOW);

        InvitePost post = new InvitePost(
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

        return new ChatRoom(post, NOW);
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