package com.opencircle.user;

import com.opencircle.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createUserGeneratesUsernameAndSavesUser() {
        AppUser user = userService.createUser(
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(1995, 1, 1),
                "San Francisco",
                "California",
                "United States"
        );

        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).matches("^[a-z]+_[a-z]+_\\d{4}$");
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPhoneNumber()).isEqualTo("+14155550123");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }
}
