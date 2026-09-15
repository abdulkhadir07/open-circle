package com.opencircle.notification;

import com.opencircle.AbstractIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RatingNotificationMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "rating_notification_migration_test";
    private static final Instant NOW = Instant.parse("2026-09-14T12:00:00Z");

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepareSchema() {
        dropSchema();
    }

    @AfterEach
    void cleanSchema() {
        dropSchema();
    }

    @Test
    void migrationPreservesExistingNotificationsAndAddsRatingTypes() {
        migrateTo("20");
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        insertUser(recipientId, "rating_migration_recipient", "rating-migration-recipient@example.com", "+14155559101");
        insertUser(actorId, "rating_migration_actor", "rating-migration-actor@example.com", "+14155559102");
        insertNotification(recipientId, actorId, "CHAT_ACTIVITY", UUID.randomUUID());

        migrateTo("21");
        insertNotification(recipientId, actorId, "RATING_REQUIRED", UUID.randomUUID());
        insertNotification(recipientId, actorId, "RATING_REVEALED", UUID.randomUUID());

        assertThat(jdbc.queryForList(
                "SELECT type FROM " + SCHEMA + ".notifications ORDER BY type",
                String.class
        )).containsExactly("CHAT_ACTIVITY", "RATING_REQUIRED", "RATING_REVEALED");
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas(SCHEMA)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion(version))
                .load()
                .migrate();
    }

    private void insertUser(UUID userId, String username, String email, String phoneNumber) {
        jdbc.update(
                """
                INSERT INTO rating_notification_migration_test.users (
                    id, username, first_name, last_name, email, password_hash,
                    phone_number, date_of_birth, city, state_region, country,
                    role, created_at, updated_at
                ) VALUES (?, ?, 'Test', 'User', ?, 'hashed-password', ?, ?,
                          'San Francisco', 'California', 'USA', 'USER', ?, ?)
                """,
                userId,
                username,
                email,
                phoneNumber,
                LocalDate.of(2000, 1, 1),
                Timestamp.from(NOW),
                Timestamp.from(NOW)
        );
    }

    private void insertNotification(UUID recipientId, UUID actorId, String type, UUID resourceId) {
        jdbc.update(
                """
                INSERT INTO rating_notification_migration_test.notifications (
                    id, recipient_user_id, actor_user_id, type, resource_type,
                    resource_id, context_type, context_id, occurrence_count,
                    occurred_at, created_at
                ) VALUES (?, ?, ?, ?, 'ENGAGEMENT_REQUEST', ?, 'INVITE_POST', ?, 1, ?, ?)
                """,
                UUID.randomUUID(),
                recipientId,
                actorId,
                type,
                resourceId,
                UUID.randomUUID(),
                Timestamp.from(NOW),
                Timestamp.from(NOW)
        );
    }

    private void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }
}
