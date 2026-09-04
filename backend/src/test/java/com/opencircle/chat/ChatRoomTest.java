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
        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
        assertThat(room.getSavedAt()).isNull();
        assertThat(room.getSavedBy()).isNull();
        assertThat(room.getAutoCloseAt()).isNull();
        assertThat(room.getClosedAt()).isNull();
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
        assertThat(room.hasActiveParticipant(requester)).isTrue();
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
    void saveKeepsRoomOpenAndClearsAutoCloseDeadline() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));

        room.leave(requester, NOW.plusSeconds(120));
        room.save(poster, NOW.plusSeconds(180));

        assertThat(room.isSaved()).isTrue();
        assertThat(room.getSavedAt()).isEqualTo(NOW.plusSeconds(180));
        assertThat(room.getSavedBy()).isEqualTo(poster);
        assertThat(room.getAutoCloseAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(NOW.plusSeconds(180));
    }

    @Test
    void saveRejectsUserWhoIsNotActiveParticipant() {
        AppUser poster = user("poster@example.com");
        AppUser outsider = user("outsider@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);

        assertThatThrownBy(() -> room.save(outsider, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only active participants can save the chat room");
    }

    @Test
    void saveRejectsClosedRoom() {
        AppUser poster = user("poster@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.close(NOW.plusSeconds(60));

        assertThatThrownBy(() -> room.save(poster, NOW.plusSeconds(120)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Closed chat rooms cannot be saved");
    }

    @Test
    void leavingRequesterStartsAutoCloseWhenOnlyOneActiveParticipantRemains() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));

        room.leave(requester, NOW.plusSeconds(120));

        assertThat(room.hasActiveParticipant(requester)).isFalse();
        assertThat(room.getAutoCloseAt()).isEqualTo(NOW.plusSeconds(120).plusSeconds(24 * 60 * 60));
        assertThat(room.getUpdatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void leavingPosterStartsAutoCloseEvenWhenRequestersRemain() {
        AppUser poster = user("poster@example.com");
        AppUser firstRequester = user("first@example.com");
        AppUser secondRequester = user("second@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(firstRequester, NOW.plusSeconds(60));
        room.addParticipant(secondRequester, NOW.plusSeconds(120));

        room.leave(poster, NOW.plusSeconds(180));

        assertThat(room.hasActiveParticipant(poster)).isFalse();
        assertThat(room.hasActiveParticipant(firstRequester)).isTrue();
        assertThat(room.hasActiveParticipant(secondRequester)).isTrue();
        assertThat(room.getAutoCloseAt()).isEqualTo(NOW.plusSeconds(180).plusSeconds(24 * 60 * 60));
    }

    @Test
    void newParticipantCancelsAutoCloseCountdownWhenRoomHasPosterAndMultipleActiveParticipants() {
        AppUser poster = user("poster@example.com");
        AppUser firstRequester = user("first@example.com");
        AppUser secondRequester = user("second@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(firstRequester, NOW.plusSeconds(60));
        room.leave(firstRequester, NOW.plusSeconds(120));

        room.addParticipant(secondRequester, NOW.plusSeconds(180));

        assertThat(room.getAutoCloseAt()).isNull();
        assertThat(room.hasActiveParticipant(poster)).isTrue();
        assertThat(room.hasActiveParticipant(secondRequester)).isTrue();
    }

    @Test
    void savedRoomDoesNotStartAutoCloseAfterParticipantLeaves() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.save(requester, NOW.plusSeconds(120));

        room.leave(requester, NOW.plusSeconds(180));

        assertThat(room.isSaved()).isTrue();
        assertThat(room.getAutoCloseAt()).isNull();
    }

    @Test
    void removeParticipantMarksRequesterRemovedAndStartsAutoCloseWhenOnlyPosterRemains() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));

        room.removeParticipant(requester, poster, NOW.plusSeconds(120));

        ChatRoomParticipant participant = onlyParticipantFor(room, requester);
        assertThat(participant.isActive()).isFalse();
        assertThat(participant.getRemovedAt()).isEqualTo(NOW.plusSeconds(120));
        assertThat(participant.getRemovedBy()).isEqualTo(poster);
        assertThat(room.getAutoCloseAt()).isEqualTo(NOW.plusSeconds(120).plusSeconds(24 * 60 * 60));
    }

    @Test
    void removeParticipantRejectsNonPoster() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        AppUser otherRequester = user("other@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.addParticipant(otherRequester, NOW.plusSeconds(120));

        assertThatThrownBy(() -> room.removeParticipant(requester, otherRequester, NOW.plusSeconds(180)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only the poster can remove chat room participants");
    }

    @Test
    void removeParticipantRejectsPosterRemovingThemselves() {
        AppUser poster = user("poster@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);

        assertThatThrownBy(() -> room.removeParticipant(poster, poster, NOW.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Poster must leave the chat room instead of removing themselves");
    }

    @Test
    void hideForOnlySetsParticipantHiddenTime() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));

        room.hideFor(requester, NOW.plusSeconds(120));

        ChatRoomParticipant participant = onlyParticipantFor(room, requester);
        assertThat(participant.getHiddenAt()).isEqualTo(NOW.plusSeconds(120));
        assertThat(participant.isActive()).isTrue();
        assertThat(room.hasActiveParticipant(requester)).isTrue();
    }

    @Test
    void closeArchivesRoomAndClearsAutoCloseDeadline() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.leave(requester, NOW.plusSeconds(120));

        room.close(NOW.plusSeconds(180));

        assertThat(room.getStatus()).isEqualTo(ChatRoomStatus.CLOSED);
        assertThat(room.isClosed()).isTrue();
        assertThat(room.getClosedAt()).isEqualTo(NOW.plusSeconds(180));
        assertThat(room.getAutoCloseAt()).isNull();
        assertThat(room.getUpdatedAt()).isEqualTo(NOW.plusSeconds(180));
    }

    @Test
    void shouldAutoCloseReturnsTrueWhenDeadlineHasPassed() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.leave(requester, NOW.plusSeconds(120));

        assertThat(room.shouldAutoClose(NOW.plusSeconds(120).plusSeconds(24 * 60 * 60))).isTrue();
    }

    @Test
    void shouldAutoCloseReturnsFalseBeforeDeadline() {
        AppUser poster = user("poster@example.com");
        AppUser requester = user("requester@example.com");
        ChatRoom room = new ChatRoom(invitePost(poster), NOW);
        room.addParticipant(poster, NOW);
        room.addParticipant(requester, NOW.plusSeconds(60));
        room.leave(requester, NOW.plusSeconds(120));

        assertThat(room.shouldAutoClose(NOW.plusSeconds(119).plusSeconds(24 * 60 * 60))).isFalse();
    }

    @Test
    void constructorRejectsMissingInvitePost() {
        assertThatThrownBy(() -> new ChatRoom(null, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invite post is required");
    }

    @Test
    void constructorRejectsMissingCreatedTime() {
        assertThatThrownBy(() -> new ChatRoom(invitePost(user("poster@example.com")), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Created time is required");
    }

    private ChatRoomParticipant onlyParticipantFor(ChatRoom room, AppUser user) {
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