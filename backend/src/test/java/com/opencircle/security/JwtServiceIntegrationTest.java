package com.opencircle.security;

import com.opencircle.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JwtServiceIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-must-be-at-least-32-characters-long",
        "app.security.jwt.expiration-minutes=15"
})
class JwtServiceIntegrationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void generateTokenCreatesTokenWithUserClaims() {
        AppUser user = new AppUser(
                "bright_river_1234",
                "Jane",
                "Doe",
                "jane@example.com",
                "hashed-password",
                "+14155550123",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        setId(user);

        String token = jwtService.generateToken(user);
        Jwt jwt = jwtService.validateToken(token);

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getClaimAsString("email")).isEqualTo("jane@example.com");
        assertThat(jwt.getClaimAsString("username")).isEqualTo("bright_river_1234");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    private void setId(AppUser user) {
        try {
            java.lang.reflect.Field id = AppUser.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(user, UUID.randomUUID());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set test user id", exception);
        }
    }

    @Import({JwtConfig.class, JwtService.class})
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }
}
