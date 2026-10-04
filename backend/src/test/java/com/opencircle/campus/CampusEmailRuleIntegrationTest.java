package com.opencircle.campus;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.mail.MailService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The test profile turns the .edu rule off so thousands of example.com fixtures keep working;
// this class turns it back on to prove the real default behaviour.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "app.campus.required-email-suffix=.edu")
class CampusEmailRuleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService users;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private MailService mailService;

    @Test
    void signupRejectsANonCampusEmail() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("jane@gmail.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Use your school email address (it must end in .edu)"));
    }

    @Test
    void signupAcceptsACampusEmailWithoutAnyLocationAndAssignsTheCampus() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("jane@student.sfsu.edu")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("jane@student.sfsu.edu"))
                .andExpect(jsonPath("$.user.campus").value("sfsu.edu"))
                .andExpect(jsonPath("$.user.city").doesNotExist())
                .andExpect(jsonPath("$.user.country").doesNotExist());
    }

    @Test
    void emailChangeRejectsANonCampusEmail() throws Exception {
        AppUser user = users.createUser(
                "Test",
                "User",
                "current@sfsu.edu",
                passwordEncoder.encode("Password123!"),
                "+14155550142",
                LocalDate.of(2000, 1, 1)
        );
        user.markEmailVerified(Instant.parse("2026-08-30T00:00:00Z"));

        String token = loginToken("current@sfsu.edu");

        mockMvc.perform(post("/api/users/me/email-change")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"me@gmail.com","currentPassword":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Use your school email address (it must end in .edu)"));
    }

    private String signupBody(String email) {
        return """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "email": "%s",
                  "password": "Password123!",
                  "phoneNumber": "+16505550193",
                  "dateOfBirth": "2000-01-01"
                }
                """.formatted(email);
    }

    private String loginToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return response.split("\"token\":\"")[1].split("\"")[0];
    }
}
