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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ChatActivityNotificationMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "chat_activity_notification_migration_test";

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
    void migrationPreservesExistingNotificationsAndAddsChatContract() {
        migrateTo("19");
        UUID recipientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        insertUser(recipientId, "migration_recipient", "migration-recipient@example.com", "+14155559011");
        insertUser(actorId, "migration_actor", "migration-actor@example.com", "+14155559012");
        insertNotification(
                UUID.randomUUID(),
                recipientId,
                actorId,
                "ENGAGEMENT_REQUESTED",
                "ENGAGEMENT_REQUEST",
                UUID.randomUUID()
        );

        migrateTo("20");
        insertNotification(
                UUID.randomUUID(),
                recipientId,
                actorId,
                "CHAT_ACTIVITY",
                "CHAT_ROOM",
                UUID.randomUUID()
        );

        assertThat(jdbc.queryForList(
                "SELECT type FROM " + SCHEMA + ".notifications ORDER BY type",
                String.class
        )).containsExactly("CHAT_ACTIVITY", "ENGAGEMENT_REQUESTED");
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

    private void insertUser(
            UUID userId,
            String username,
            String email,
            String phoneNumber
    ) {
        Instant now = Instant.parse("2026-09-14T12:00:00Z");
        jdbc.update(
                """
                INSERT INTO chat_activity_notification_migration_test.users (
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
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertNotification(
            UUID notificationId,
            UUID recipientId,
            UUID actorId,
            String type,
            String resourceType,
            UUID resourceId
    ) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        jdbc.update(
                """
                INSERT INTO chat_activity_notification_migration_test.notifications (
                    id, recipient_user_id, actor_user_id, type, resource_type,
                    resource_id, occurrence_count, occurred_at, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)
                """,
                notificationId,
                recipientId,
                actorId,
                type,
                resourceType,
                resourceId,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }
}
