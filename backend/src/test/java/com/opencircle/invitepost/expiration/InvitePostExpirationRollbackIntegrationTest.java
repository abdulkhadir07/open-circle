package com.opencircle.invitepost.expiration;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.invitepost.InvitePost;
import com.opencircle.invitepost.InvitePostRepository;
import com.opencircle.invitepost.InviteType;
import com.opencircle.invitepost.LocationScope;
import com.opencircle.notification.NotificationCommand;
import com.opencircle.notification.NotificationPublisher;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class InvitePostExpirationRollbackIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private InvitePostExpirationProcessor processor;
    @Autowired private InvitePostExpirationRepository expirations;
    @Autowired private InvitePostRepository invitePosts;
    @Autowired private UserService users;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean
    private NotificationPublisher notificationPublisher;

    @Test
    void notificationFailureRollsBackClaimSoPostCanBeRetried() {
        InvitePost post = invitePosts.saveAndFlush(new InvitePost(
                user("rollback-poster"),
                "Expiration rollback test",
                InviteType.SINGLE,
                1,
                LocationScope.CITY,
                "San Francisco",
                "California",
                "USA",
                NOW.minusSeconds(25 * 60 * 60L)
        ));
        InvitePostExpirationCandidate candidate = expirations.findDue(NOW).stream()
                .filter(value -> value.postId().equals(post.getId()))
                .findFirst()
                .orElseThrow();
        doThrow(new IllegalStateException("notification insert failed"))
                .when(notificationPublisher)
                .publish(any(NotificationCommand.class));

        assertThatThrownBy(() -> processor.process(candidate, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("notification insert failed");
        assertThat(jdbc.queryForObject(
                "SELECT expiration_notified_at FROM invite_posts WHERE id = ?",
                Instant.class,
                post.getId()
        )).isNull();
        assertThat(expirations.findDue(NOW))
                .extracting(InvitePostExpirationCandidate::postId)
                .contains(post.getId());
    }

    private AppUser user(String label) {
        String unique = label + "." + Long.toUnsignedString(System.nanoTime());
        return users.createUser(
                "Test",
                "User",
                unique + "@example.com",
                "hashed-password",
                "+1415" + Long.toUnsignedString(System.nanoTime()),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
