package com.opencircle.user;

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
class UserProfileMigrationIntegrationTest extends AbstractIntegrationTest {

    private static final String SCHEMA = "user_profile_migration_test";

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
    void migrationBackfillsExistingUsersAndFallsBackForBlankLegacyFirstName() {
        migrateTo("14");
        UUID namedUserId = UUID.randomUUID();
        UUID blankNameUserId = UUID.randomUUID();
        insertLegacyUser(namedUserId, "named_user_1234", "  Jordan  ", "named@example.com", "+14155558001");
        insertLegacyUser(blankNameUserId, "fallback_user_1234", "   ", "fallback@example.com", "+14155558002");

        migrateTo("15");

        assertThat(profileDisplayName(namedUserId)).isEqualTo("Jordan");
        assertThat(profileDisplayName(blankNameUserId)).isEqualTo("fallback_user_1234");
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

    private void insertLegacyUser(
            UUID id,
            String username,
            String firstName,
            String email,
            String phoneNumber
    ) {
        Instant now = Instant.parse("2026-09-12T12:00:00Z");
        jdbc.update(
                """
                INSERT INTO user_profile_migration_test.users (
                    id,
                    username,
                    first_name,
                    last_name,
                    email,
                    password_hash,
                    phone_number,
                    date_of_birth,
                    city,
                    state_region,
                    country,
                    role,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, 'User', ?, 'hashed-password', ?, ?,
                        'San Francisco', 'California', 'USA', 'USER', ?, ?)
                """,
                id,
                username,
                firstName,
                email,
                phoneNumber,
                LocalDate.of(2000, 1, 1),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private String profileDisplayName(UUID userId) {
        return jdbc.queryForObject(
                "SELECT display_name FROM user_profile_migration_test.user_profiles WHERE user_id = ?",
                String.class,
                userId
        );
    }

    private void dropSchema() {
        jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
    }
}
