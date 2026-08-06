package com.opencircle.user;

import com.opencircle.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Test
    void duplicateEmailViolatesUniqueConstraint() {
        users.saveAndFlush(user("user_one_1001", "same@example.com", "+10000000001"));

        assertThatThrownBy(() ->
                users.saveAndFlush(user("user_two_1002", "same@example.com", "+10000000002")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateUsernameViolatesUniqueConstraint() {
        users.saveAndFlush(user("same_user_1001", "one@example.com", "+10000000003"));

        assertThatThrownBy(() ->
                users.saveAndFlush(user("same_user_1001", "two@example.com", "+10000000004")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePhoneNumberViolatesUniqueConstraint() {
        users.saveAndFlush(user("phone_one_1001", "three@example.com", "+19999999999"));

        assertThatThrownBy(() ->
                users.saveAndFlush(user("phone_two_1002", "four@example.com", "+19999999999")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AppUser user(String username, String email, String phoneNumber) {
        return new AppUser(
                username,
                "Jane",
                "Doe",
                email,
                "hashed-pw",
                phoneNumber,
                LocalDate.of(1995, 1, 1),
                "San Francisco",
                "California",
                "United States"
        );
    }
}
