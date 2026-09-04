package com.opencircle.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = SecurityConfigIntegrationTest.TestApp.class,
        properties = {
                "app.security.jwt.secret=test-secret-must-be-at-least-32-characters-long",
                "app.security.jwt.expiration-minutes=60",
                "app.cors.allowed-origins=http://localhost:5173"
        }
)
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @MethodSource("protectedRoutes")
    void protectedRoutesReturnCustomUnauthorizedErrorWhenMissingToken(HttpMethod method, String path) throws Exception {
        mockMvc.perform(request(method, path)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value(path));
    }

    private static Stream<Arguments> protectedRoutes() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/protected-test"),
                Arguments.of(HttpMethod.GET, "/api/users/me"),
                Arguments.of(HttpMethod.PUT, "/api/users/me/location"),

                Arguments.of(HttpMethod.POST, "/api/invite-posts"),
                Arguments.of(HttpMethod.GET, "/api/invite-posts/local"),
                Arguments.of(HttpMethod.GET, "/api/invite-posts/global"),

                Arguments.of(HttpMethod.POST, "/api/invite-posts/00000000-0000-0000-0000-000000000001/engagements"),
                Arguments.of(HttpMethod.GET, "/api/invite-posts/00000000-0000-0000-0000-000000000001/engagements"),
                Arguments.of(HttpMethod.PATCH, "/api/engagements/00000000-0000-0000-0000-000000000001/accept"),
                Arguments.of(HttpMethod.PATCH, "/api/engagements/00000000-0000-0000-0000-000000000001/decline"),
                Arguments.of(HttpMethod.PATCH, "/api/engagements/00000000-0000-0000-0000-000000000001/hold"),
                Arguments.of(HttpMethod.PATCH, "/api/engagements/00000000-0000-0000-0000-000000000001/withdraw"),

                Arguments.of(HttpMethod.GET, "/api/chat-rooms"),
                Arguments.of(HttpMethod.GET, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/messages"),
                Arguments.of(HttpMethod.POST, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/messages"),
                Arguments.of(HttpMethod.PATCH, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/save"),
                Arguments.of(HttpMethod.PATCH, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/leave"),
                Arguments.of(HttpMethod.PATCH, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/hide"),
                Arguments.of(HttpMethod.PATCH, "/api/chat-rooms/00000000-0000-0000-0000-000000000001/participants/00000000-0000-0000-0000-000000000002/remove")
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class
    })
    @EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
    @Import({
            SecurityConfig.class,
            JwtConfig.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class,
            ProtectedTestController.class
    })
    static class TestApp {
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/api/protected-test")
        Map<String, String> protectedTest() {
            // Provides a protected endpoint used to verify unauthorized JSON errors.
            return Map.of("status", "ok");
        }
    }
}