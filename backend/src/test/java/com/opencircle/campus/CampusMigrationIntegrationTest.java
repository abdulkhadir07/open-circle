package com.opencircle.campus;

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
class CampusMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "campus_migration_test";

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
    void migrationBackfillsCampusFromEmailDomainsAndAllowsCampusPostsWithoutALocation() {
        migrateTo("24");
        UUID sfsuUser = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID oldPost = UUID.randomUUID();
        insertUser(sfsuUser, "legacy_sfsu", "Jane@Student.SFSU.edu", "+14155559101");
        insertUser(otherUser, "legacy_other", "sam@example.com", "+14155559102");
        insertPost(oldPost, sfsuUser);

        migrateTo("25");

        assertThat(userCampus(sfsuUser)).isEqualTo("sfsu.edu");
        assertThat(userCampus(otherUser)).isEqualTo("example.com");
        assertThat(postCampus(oldPost)).isEqualTo("sfsu.edu");

        // City/country are optional now and CAMPUS is an accepted scope.
        UUID campusPost = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update(
                """
                INSERT INTO campus_migration_test.invite_posts (
                    id, poster_id, content, invite_type, total_capacity, accepted_count,
                    location_scope, campus, status, expires_at, created_at, updated_at
                ) VALUES (?, ?, 'Campus post', 'SINGLE', 1, 0, 'CAMPUS', 'sfsu.edu',
                          'ACTIVE', ?, ?, ?)
                """,
                campusPost,
                sfsuUser,
                Timestamp.from(now.plusSeconds(3600)),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        assertThat(postCampus(campusPost)).isEqualTo("sfsu.edu");
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

    private void insertUser(UUID id, String username, String email, String phone) {
        Instant now = Instant.parse("2026-09-14T12:00:00Z");
        jdbc.update(
                """
                INSERT INTO campus_migration_test.users (
                    id, username, first_name, last_name, email, password_hash,
                    phone_number, date_of_birth, city, state_region, country,
                    role, created_at, updated_at
                ) VALUES (?, ?, 'Test', 'User', ?, 'hashed-password', ?, ?,
                          'San Francisco', 'California', 'USA', 'USER', ?, ?)
                """,
                id,
                username,
                email.toLowerCase(),
                phone,
                LocalDate.of(2000, 1, 1),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertPost(UUID postId, UUID posterId) {
        Instant createdAt = Instant.parse("2026-09-14T12:00:00Z");
        jdbc.update(
                """
                INSERT INTO campus_migration_test.invite_posts (
                    id, poster_id, content, invite_type, total_capacity,
                    accepted_count, location_scope, city, state_region, country,
                    status, expires_at, created_at, updated_at
                ) VALUES (?, ?, 'Legacy post', 'SINGLE', 1, 0,
                          'CITY', 'San Francisco', 'California', 'USA', 'ACTIVE',
                          ?, ?, ?)
                """,
                postId,
                posterId,
                Timestamp.from(createdAt.plusSeconds(24 * 60 * 60L)),
                Timestamp.from(createdAt),
                Timestamp.from(createdAt)
        );
    }

    private String userCampus(UUID id) {
        return jdbc.queryForObject(
                "SELECT campus FROM campus_migration_test.users WHERE id = ?", String.class, id);
    }

    private String postCampus(UUID id) {
        return jdbc.queryForObject(
                "SELECT campus FROM campus_migration_test.invite_posts WHERE id = ?", String.class, id);
    }

    private void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }
}
