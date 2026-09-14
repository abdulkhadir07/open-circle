package com.opencircle.accountsettings;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EmailChangeRequestRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");

    @Autowired private EmailChangeRequestRepository emailChanges;
    @Autowired private UserService userService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void databaseAllowsOnlyOneActiveEmailChangePerUser() {
        AppUser user = createUser("single-request");
        emailChanges.saveAndFlush(request(user, "first@example.com", "a"));

        assertThatThrownBy(() -> emailChanges.saveAndFlush(
                request(user, "second@example.com", "b")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void databaseEnforcesCaseInsensitiveUserEmailUniqueness() {
        AppUser first = createUser("email-case-first");
        AppUser second = createUser("email-case-second");
        userService.saveAndFlush(first);
        userService.saveAndFlush(second);
        jdbcTemplate.update(
                "UPDATE users SET email = ? WHERE id = ?",
                "CaseSensitive@Example.com",
                first.getId()
        );

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE users SET email = ? WHERE id = ?",
                "casesensitive@example.com",
                second.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private EmailChangeRequest request(AppUser user, String email, String hashSeed) {
        return new EmailChangeRequest(
                user,
                email,
                hashSeed.repeat(60),
                NOW,
                NOW.plusSeconds(15 * 60)
        );
    }

    private AppUser createUser(String suffix) {
        return userService.createUser(
                "Settings",
                "Tester",
                suffix + "@example.com",
                "hashed-password",
                "+1415555" + Math.abs(suffix.hashCode() % 10000),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
    }
}
