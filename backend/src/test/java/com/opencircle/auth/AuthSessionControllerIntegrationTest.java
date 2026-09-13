package com.opencircle.auth;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.mail.MailService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private MailService mailService;

    @Test
    void signupAndFailedVerificationCreateNoSession() throws Exception {
        signup("pending.session@example.com", "+14155550300");

        assertThat(sessionCount("pending.session@example.com")).isZero();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "pending.session@example.com",
                                  "code": "000000"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(sessionCount("pending.session@example.com")).isZero();
    }

    @Test
    void verificationCreatesFirstSessionAndSecurelyScopedRefreshCookie() throws Exception {
        signup("verified.session@example.com", "+14155550301");

        MvcResult result = verifyEmail("verified.session@example.com");

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("open_circle_refresh=")
                .contains("HttpOnly")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");

        assertThat(sessionCount("verified.session@example.com")).isEqualTo(1);
    }

    @Test
    void loginCreatesIndependentSessionForVerifiedUser() throws Exception {
        signup("login.session@example.com", "+14155550302");
        verifyEmail("login.session@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.USER_AGENT, "OpenCircle Browser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login.session@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("open_circle_refresh=")));

        Integer sessionCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM auth_sessions sessions
                        JOIN users users ON users.id = sessions.user_id
                        WHERE users.email = ? AND sessions.revoked_at IS NULL
                        """,
                Integer.class,
                "login.session@example.com"
        );
        assertThat(sessionCount).isEqualTo(2);
    }

    @Test
    void refreshRotatesTokenAndReplayRevokesThatSession() throws Exception {
        signup("rotate.controller@example.com", "+14155550303");
        Cookie originalCookie = refreshCookie(verifyEmail("rotate.controller@example.com"));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(originalCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn();

        Cookie rotatedCookie = refreshCookie(refreshed);
        assertThat(rotatedCookie.getValue()).isNotEqualTo(originalCookie.getValue());

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(originalCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesCurrentSessionAndClearsCookieIdempotently() throws Exception {
        signup("logout.controller@example.com", "+14155550304");
        Cookie refreshCookie = refreshCookie(verifyEmail("logout.controller@example.com"));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetRevokesEverySessionForTheAccount() throws Exception {
        signup("reset.sessions@example.com", "+14155550305");
        Cookie verificationSession = refreshCookie(verifyEmail("reset.sessions@example.com"));

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset.sessions@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        Cookie loginSession = refreshCookie(login);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "reset.sessions@example.com"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reset.sessions@example.com",
                                  "code": "%s",
                                  "newPassword": "NewPassword123!"
                                }
                                """.formatted(latestPasswordResetCode())))
                .andExpect(status().isNoContent());

        assertRefreshRejected(verificationSession);
        assertRefreshRejected(loginSession);
    }

    @Test
    void refreshAndLogoutRejectUntrustedBrowserOrigin() throws Exception {
        signup("origin.session@example.com", "+14155550306");
        Cookie refreshCookie = refreshCookie(verifyEmail("origin.session@example.com"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .cookie(refreshCookie))
                .andExpect(status().isForbidden());
    }

    private void signup(String email, String phoneNumber) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Session",
                                  "lastName": "Tester",
                                  "email": "%s",
                                  "password": "Password123!",
                                  "phoneNumber": "%s",
                                  "dateOfBirth": "2000-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """.formatted(email, phoneNumber)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private MvcResult verifyEmail(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                        .header(HttpHeaders.USER_AGENT, "OpenCircle Test Browser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "code": "%s"
                                }
                                """.formatted(email, latestVerificationCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn();
    }

    private void assertRefreshRejected(Cookie refreshCookie) throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    private Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("open_circle_refresh");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private String latestVerificationCode() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendEmailVerificationCode(
                org.mockito.ArgumentMatchers.anyString(),
                code.capture()
        );
        List<String> values = code.getAllValues();
        return values.getLast();
    }

    private String latestPasswordResetCode() {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendPasswordResetCode(
                org.mockito.ArgumentMatchers.anyString(),
                code.capture()
        );
        List<String> values = code.getAllValues();
        return values.getLast();
    }

    private Integer sessionCount(String email) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM auth_sessions sessions
                        JOIN users users ON users.id = sessions.user_id
                        WHERE users.email = ?
                        """,
                Integer.class,
                email
        );
    }
}
