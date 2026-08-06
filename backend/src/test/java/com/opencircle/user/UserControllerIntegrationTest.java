package com.opencircle.user;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @Test
    void meReturnsCurrentUserWhenAuthenticated() throws Exception {
        AppUser user = userService.createUser(
                "Current",
                "User",
                "current.user@example.com",
                "hashed-password",
                "+14155550300",
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );

        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.username", notNullValue()))
                .andExpect(jsonPath("$.firstName").value("Current"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("current.user@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+14155550300"))
                .andExpect(jsonPath("$.dateOfBirth").value("2000-01-01"))
                .andExpect(jsonPath("$.city").value("San Francisco"))
                .andExpect(jsonPath("$.stateRegion").value("California"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void meRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
