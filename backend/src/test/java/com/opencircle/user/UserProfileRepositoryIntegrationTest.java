package com.opencircle.user;

import com.opencircle.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserProfileRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger(6000);

    @Autowired private UserService users;
    @Autowired private UserProfileRepository profiles;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void storesAndReadsInterestsByExplicitDisplayOrder() {
        AppUser user = user("ordered");
        UserProfile profile = profiles.findForUpdate(user.getId()).orElseThrow();
        profile.replace(
                "Ordered Profile",
                "A short bio",
                List.of("Third Place", "First Place", "Second Place"),
                Instant.parse("2026-09-12T13:00:00Z")
        );
        entityManager.flush();
        entityManager.clear();

        UserProfile reloaded = profiles.findForDisplay(user.getId()).orElseThrow();

        assertThat(reloaded.getDisplayName()).isEqualTo("Ordered Profile");
        assertThat(reloaded.getInterests())
                .containsExactly("Third Place", "First Place", "Second Place");
        assertThat(jdbc.queryForList(
                """
                SELECT display_order
                FROM user_profile_interests
                WHERE user_id = ?
                ORDER BY display_order
                """,
                Short.class,
                user.getId()
        )).containsExactly((short) 0, (short) 1, (short) 2);
    }

    @Test
    void databaseRejectsCaseInsensitiveDuplicateInterests() {
        AppUser user = user("duplicate");
        jdbc.update(
                "INSERT INTO user_profile_interests (user_id, interest, display_order) VALUES (?, 'Hiking', 0)",
                user.getId()
        );

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_profile_interests (user_id, interest, display_order) VALUES (?, 'hIkInG', 1)",
                user.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsInterestOrderOutsideSupportedRange() {
        AppUser user = user("order-limit");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_profile_interests (user_id, interest, display_order) VALUES (?, 'Reading', 8)",
                user.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUser user(String label) {
        int sequence = USER_SEQUENCE.incrementAndGet();
        AppUser user = users.createUser(
                "Profile",
                "User",
                label + ".profile.repository." + sequence + "@example.com",
                "hashed-password",
                "+1415555" + sequence,
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        entityManager.flush();
        return user;
    }
}
