package com.opencircle.invitepost.expiration;

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
class InvitePostExpirationMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "invite_expiration_migration_test";

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
    void migrationSuppressesRetroactiveNotificationsButLeavesFuturePostsEligible() {
        migrateTo("18");
        UUID posterId = UUID.randomUUID();
        UUID expiredPostId = UUID.randomUUID();
        UUID futurePostId = UUID.randomUUID();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant expiredAt = now.minusSeconds(60 * 60);
        Instant futureExpiration = now.plusSeconds(60 * 60);
        insertUser(posterId);
        insertPost(expiredPostId, posterId, expiredAt);
        insertPost(futurePostId, posterId, futureExpiration);

        migrateTo("19");

        assertThat(expirationNotifiedAt(expiredPostId)).isEqualTo(expiredAt);
        assertThat(expirationNotifiedAt(futurePostId)).isNull();
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

    private void insertUser(UUID userId) {
        Instant now = Instant.parse("2026-09-14T12:00:00Z");
        jdbc.update(
                """
                INSERT INTO invite_expiration_migration_test.users (
                    id, username, first_name, last_name, email, password_hash,
                    phone_number, date_of_birth, city, state_region, country,
                    role, created_at, updated_at
                ) VALUES (?, 'expiration_migration_user', 'Test', 'User',
                          'expiration-migration@example.com', 'hashed-password',
                          '+14155559001', ?, 'San Francisco', 'California', 'USA',
                          'USER', ?, ?)
                """,
                userId,
                LocalDate.of(2000, 1, 1),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertPost(UUID postId, UUID posterId, Instant expiresAt) {
        Instant createdAt = expiresAt.minusSeconds(24 * 60 * 60L);
        jdbc.update(
                """
                INSERT INTO invite_expiration_migration_test.invite_posts (
                    id, poster_id, content, invite_type, total_capacity,
                    accepted_count, location_scope, city, state_region, country,
                    status, expires_at, created_at, updated_at
                ) VALUES (?, ?, 'Migration expiration test', 'SINGLE', 1, 0,
                          'CITY', 'San Francisco', 'California', 'USA', 'ACTIVE',
                          ?, ?, ?)
                """,
                postId,
                posterId,
                Timestamp.from(expiresAt),
                Timestamp.from(createdAt),
                Timestamp.from(createdAt)
        );
    }

    private Instant expirationNotifiedAt(UUID postId) {
        return jdbc.queryForObject(
                """
                SELECT expiration_notified_at
                FROM invite_expiration_migration_test.invite_posts
                WHERE id = ?
                """,
                Instant.class,
                postId
        );
    }

    private void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }
}
