package com.opencircle.chat;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.notification.NotificationBroadcaster;
import com.opencircle.storage.StorageService;
import com.opencircle.storage.StoredFile;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ChatActivityNotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserService users;
    @Autowired private InvitePostRepository posts;
    @Autowired private ChatRoomRepository rooms;
    @Autowired private ChatMessageRepository messages;
    @Autowired private ChatRoomService chatRoomService;
    @Autowired private ChatAttachmentService chatAttachmentService;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private ChatRoomPresence presence;
    @MockitoBean private NotificationBroadcaster broadcaster;
    @MockitoBean private StorageService storage;

    private final List<Fixture> createdFixtures = new ArrayList<>();

    @BeforeEach
    void defaultToAwayRecipientsAndStoredAttachments() {
        when(presence.isPresent(any(), any())).thenReturn(false);
        when(storage.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn(new StoredFile("chat-bucket", "chat-key", "image/png", 7L));
    }

    @AfterEach
    void cleanupCommittedFixtures() {
        for (Fixture fixture : createdFixtures) {
            jdbc.update("DELETE FROM notifications WHERE resource_type = 'CHAT_ROOM' AND resource_id = ?", fixture.roomId());
            jdbc.update("DELETE FROM chat_rooms WHERE id = ?", fixture.roomId());
            jdbc.update("DELETE FROM invite_posts WHERE id = ?", fixture.postId());
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", fixture.sender().getId(), fixture.recipient().getId());
        }
        createdFixtures.clear();
    }

    @Test
    void textAndAttachmentMessagesUseTheSameAggregatedNotification() {
        Fixture fixture = fixture();

        chatRoomService.sendMessage(fixture.sender(), fixture.roomId(), "Private message body");
        chatAttachmentService.uploadAttachment(
                fixture.sender(),
                fixture.roomId(),
                new ChatAttachmentUpload(
                        "private-file.png",
                        "image/png",
                        7L,
                        new ByteArrayInputStream(new byte[7]),
                        "Private caption"
                )
        );

        Map<String, Object> row = notification(fixture.recipient().getId(), fixture.roomId());
        assertThat(row)
                .containsEntry("type", "CHAT_ACTIVITY")
                .containsEntry("resource_type", "CHAT_ROOM")
                .containsEntry("resource_id", fixture.roomId())
                .containsEntry("context_type", "INVITE_POST")
                .containsEntry("context_id", fixture.postId())
                .containsEntry("actor_user_id", fixture.sender().getId())
                .containsEntry("occurrence_count", 2)
                .containsEntry("read_at", null);
        assertThat(notificationCount(fixture.sender().getId(), fixture.roomId())).isZero();
    }

    @Test
    void recipientPresentInTheRoomDoesNotReceiveActivityNotification() {
        Fixture fixture = fixture();
        when(presence.isPresent(fixture.recipient().getId(), fixture.roomId())).thenReturn(true);

        chatRoomService.sendMessage(fixture.sender(), fixture.roomId(), "Visible in the open room");

        assertThat(notificationCount(fixture.recipient().getId(), fixture.roomId())).isZero();
    }

    @Test
    void messageRollbackAlsoRollsBackItsActivityNotification() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            chatRoomService.sendMessage(fixture.sender(), fixture.roomId(), "Rolled back message");
            throw new ExpectedRollbackException();
        })).isInstanceOf(ExpectedRollbackException.class);

        assertThat(notificationCount(fixture.recipient().getId(), fixture.roomId())).isZero();
        ChatRoom room = rooms.findById(fixture.roomId()).orElseThrow();
        assertThat(messages.findByChatRoomOrderByCreatedAtAscIdAsc(room)).isEmpty();
    }

    private Fixture fixture() {
        Fixture fixture = transactions.execute(status -> {
            AppUser sender = user("activity-integration-sender");
            AppUser recipient = user("activity-integration-recipient");
            Instant createdAt = Instant.now().minusSeconds(60);
            InvitePost post = posts.save(new InvitePost(
                    sender,
                    "Chat activity integration",
                    InviteType.GROUP,
                    3,
                    LocationScope.CITY,
                    "San Francisco",
                    "California",
                    "USA",
                    createdAt
            ));
            ChatRoom room = new ChatRoom(post, createdAt);
            room.addParticipant(sender, createdAt);
            room.addParticipant(recipient, createdAt.plusSeconds(1));
            room = rooms.save(room);
            return new Fixture(sender, recipient, room.getId(), post.getId());
        });
        createdFixtures.add(fixture);
        return fixture;
    }

    private Map<String, Object> notification(UUID recipientUserId, UUID roomId) {
        return jdbc.queryForMap(
                """
                SELECT type, resource_type, resource_id, context_type, context_id,
                       actor_user_id, occurrence_count, read_at
                FROM notifications
                WHERE recipient_user_id = ?
                  AND type = 'CHAT_ACTIVITY'
                  AND resource_type = 'CHAT_ROOM'
                  AND resource_id = ?
                """,
                recipientUserId,
                roomId
        );
    }

    private int notificationCount(UUID recipientUserId, UUID roomId) {
        return jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM notifications
                WHERE recipient_user_id = ?
                  AND type = 'CHAT_ACTIVITY'
                  AND resource_type = 'CHAT_ROOM'
                  AND resource_id = ?
                """,
                Integer.class,
                recipientUserId,
                roomId
        );
    }

    private AppUser user(String label) {
        String unique = Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                label + "." + unique + "@example.com",
                "hashed-password",
                "+1%010d".formatted(Integer.toUnsignedLong((label + unique).hashCode()) % 10_000_000_000L),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }

    private record Fixture(AppUser sender, AppUser recipient, UUID roomId, UUID postId) {
    }

    private static class ExpectedRollbackException extends RuntimeException {
    }
}
