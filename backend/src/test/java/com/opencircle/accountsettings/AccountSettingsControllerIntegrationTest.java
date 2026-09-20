package com.opencircle.accountsettings;

import com.opencircle.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import com.opencircle.mail.MailService;
import com.opencircle.passwordreset.PasswordResetService;
import com.opencircle.security.JwtService;
import com.opencircle.session.IssuedSession;
import com.opencircle.session.SessionService;
import com.opencircle.user.AppUser;
import com.opencircle.user.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSettingsControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(6_000_000);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private SessionService sessionService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private MailService mailService;

    @Test
    void settingsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me/sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void settingsRejectAccessTokensWithoutSessionIdentity() throws Exception {
        TestAccount account = createAccount("missing-session-id", "Current Browser");
        String accessTokenWithoutSession = jwtService.generateToken(account.user());

        mockMvc.perform(get("/api/users/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenWithoutSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        "Current session is unavailable; please sign in again"
                ));
    }

    @Test
    void listsCurrentAndOtherActiveSessionsWithoutSensitiveCredentials() throws Exception {
        TestAccount account = createAccount("list-sessions", "Current Browser");
        sessionService.create(account.user(), "Other Browser");

        mockMvc.perform(get("/api/users/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessions", hasSize(2)))
                .andExpect(jsonPath("$.sessions[0].id").value(account.session().sessionId().toString()))
                .andExpect(jsonPath("$.sessions[0].userAgent").value("Current Browser"))
                .andExpect(jsonPath("$.sessions[0].current").value(true))
                .andExpect(jsonPath("$.sessions[0].inactiveAt").exists())
                .andExpect(jsonPath("$.sessions[0].expiresAt").exists())
                .andExpect(jsonPath("$.sessions[0].refreshToken").doesNotExist())
                .andExpect(jsonPath("$.sessions[0].tokenHash").doesNotExist());
    }

    @Test
    void revokesOneOwnedSessionButNotTheCurrentSession() throws Exception {
        TestAccount account = createAccount("revoke-one", "Current Browser");
        IssuedSession other = sessionService.create(account.user(), "Other Browser");

        mockMvc.perform(delete("/api/users/me/sessions/{sessionId}", other.sessionId())
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertRefreshRejected(other.refreshToken());
        assertThat(activeSessionCount(account.user().getId())).isEqualTo(1);
    }

    @Test
    void cannotRevokeAnotherUsersSession() throws Exception {
        TestAccount account = createAccount("revoke-owner", "Owner Browser");
        TestAccount other = createAccount("revoke-foreign", "Foreign Browser");

        mockMvc.perform(delete("/api/users/me/sessions/{sessionId}", other.session().sessionId())
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Session not found"));

        assertThat(activeSessionCount(other.user().getId())).isEqualTo(1);
    }

    @Test
    void revokingCurrentSessionClearsItsCookie() throws Exception {
        TestAccount account = createAccount("revoke-current", "Current Browser");

        mockMvc.perform(delete("/api/users/me/sessions/{sessionId}", account.session().sessionId())
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertRefreshRejected(account.session().refreshToken());
    }

    @Test
    void logoutAllRevokesEverySessionAndClearsCookie() throws Exception {
        TestAccount account = createAccount("logout-all", "Current Browser");
        IssuedSession other = sessionService.create(account.user(), "Other Browser");

        mockMvc.perform(delete("/api/users/me/sessions")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertRefreshRejected(account.session().refreshToken());
        assertRefreshRejected(other.refreshToken());
    }

    @Test
    void passwordChangeRotatesCurrentRevokesOthersAndInvalidatesResetCode() throws Exception {
        TestAccount account = createAccount("password-change", "Current Browser");
        IssuedSession other = sessionService.create(account.user(), "Other Browser");
        passwordResetService.requestReset(account.user().getEmail());

        MvcResult result = mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Password123!",
                                  "newPassword": "NewPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn();

        Cookie rotatedCookie = refreshCookie(result);
        assertThat(jwtService.validateToken(JsonPath.read(
                        result.getResponse().getContentAsString(),
                        "$.token"
                ))
                .getClaimAsString("sid"))
                .isEqualTo(account.session().sessionId().toString());
        assertThat(passwordEncoder.matches(
                "NewPassword123!",
                userService.getById(account.user().getId()).getPasswordHash()
        )).isTrue();
        assertThat(unusedPasswordResetCodeCount(account.user().getId())).isZero();
        assertRefreshRejected(other.refreshToken());
        assertRefreshAccepted(rotatedCookie);
    }

    @Test
    void passwordChangeRejectsWrongOrReusedPasswordWithoutRevokingSession() throws Exception {
        TestAccount account = createAccount("password-invalid", "Current Browser");

        mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongPassword!","newPassword":"NewPassword123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));

        mockMvc.perform(put("/api/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"Password123!","newPassword":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "New password must be different from the current password"
                ));

        assertThat(activeSessionCount(account.user().getId())).isEqualTo(1);
    }

    @Test
    void setHiddenChatsPinStoresOnlyHashedPin() throws Exception {
        TestAccount account = createAccount("pin-set", "Current Browser");

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","pin":"4321"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT hidden_chats_pin_hash FROM users WHERE id = ?",
                String.class,
                account.user().getId()
        );
        assertThat(storedHash).isNotEqualTo("4321");
        assertThat(passwordEncoder.matches("4321", storedHash)).isTrue();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasHiddenChatsPin").value(true));
    }

    @Test
    void setHiddenChatsPinSucceedsWithoutACurrentPasswordOnFirstSetup() throws Exception {
        TestAccount account = createAccount("pin-first-setup", "Current Browser");

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pin":"4321"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasHiddenChatsPin").value(true));
    }

    @Test
    void setHiddenChatsPinRejectsIncorrectCurrentPasswordWhenChangingAnExistingPin() throws Exception {
        TestAccount account = createAccount("pin-wrong-password", "Current Browser");

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","pin":"4321"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"WrongPassword!","pin":"8765"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void setHiddenChatsPinRejectsNonNumericOrWrongLengthPins() throws Exception {
        TestAccount account = createAccount("pin-invalid-format", "Current Browser");

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","pin":"12"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/users/me/hidden-chats-pin")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","pin":"abcd"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailChangeRequiresPasswordAndStoresOnlyHashedCode() throws Exception {
        TestAccount account = createAccount("email-request", "Current Browser");

        mockMvc.perform(post("/api/users/me/email-change")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"NEW.Email@Example.COM","currentPassword":"Password123!"}
                                """))
                .andExpect(status().isNoContent());

        String rawCode = latestEmailChangeCode("new.email@example.com");
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT code_hash FROM email_change_requests WHERE user_id = ? AND used_at IS NULL",
                String.class,
                account.user().getId()
        );
        assertThat(storedHash).isNotEqualTo(rawCode);
        assertThat(passwordEncoder.matches(rawCode, storedHash)).isTrue();

        mockMvc.perform(post("/api/users/me/email-change")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"another@example.com","currentPassword":"wrong"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void requestingAnotherEmailChangeReplacesThePendingRequest() throws Exception {
        TestAccount account = createAccount("email-replace", "Current Browser");
        requestEmailChange(account, "first-change@example.com");

        requestEmailChange(account, "second-change@example.com");

        Integer activeRequests = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_change_requests WHERE user_id = ? AND used_at IS NULL",
                Integer.class,
                account.user().getId()
        );
        String activeEmail = jdbcTemplate.queryForObject(
                "SELECT new_email FROM email_change_requests WHERE user_id = ? AND used_at IS NULL",
                String.class,
                account.user().getId()
        );
        Integer replacedRequests = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_change_requests WHERE user_id = ? AND used_at IS NOT NULL",
                Integer.class,
                account.user().getId()
        );

        assertThat(activeRequests).isEqualTo(1);
        assertThat(activeEmail).isEqualTo("second-change@example.com");
        assertThat(replacedRequests).isEqualTo(1);
    }

    @Test
    void verifiedEmailChangeUpdatesIdentityAndSecuresSessions() throws Exception {
        TestAccount account = createAccount("email-verify", "Current Browser");
        IssuedSession other = sessionService.create(account.user(), "Other Browser");
        passwordResetService.requestReset(account.user().getEmail());
        requestEmailChange(account, "New.Identity@Example.com");
        String code = latestEmailChangeCode("new.identity@example.com");

        MvcResult result = mockMvc.perform(post("/api/users/me/email-change/verify")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andReturn();

        AppUser updated = userService.getById(account.user().getId());
        assertThat(updated.getEmail()).isEqualTo("new.identity@example.com");
        assertThat(updated.isEmailVerified()).isTrue();
        assertThat(unusedPasswordResetCodeCount(updated.getId())).isZero();
        assertRefreshRejected(other.refreshToken());
        assertRefreshAccepted(refreshCookie(result));
        verify(mailService).sendEmailChangedNotice(
                account.user().getEmail(),
                "new.identity@example.com"
        );
    }

    @Test
    void failedEmailCodesPersistAttemptsAndEventuallyLockTheRequest() throws Exception {
        TestAccount account = createAccount("email-attempts", "Current Browser");
        requestEmailChange(account, "attempts@example.com");

        for (int attempt = 1; attempt <= 5; attempt++) {
            var expectation = mockMvc.perform(post("/api/users/me/email-change/verify")
                            .header(HttpHeaders.AUTHORIZATION, account.authorization())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"code":"000000"}
                                    """))
                    .andExpect(status().isBadRequest());

            if (attempt < 5) {
                expectation.andExpect(jsonPath("$.message").value("Invalid or expired email change code"));
            } else {
                expectation.andExpect(jsonPath("$.message").value(
                        "Email change attempts exceeded. Please request a new code"
                ));
            }
        }

        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT attempt_count FROM email_change_requests WHERE user_id = ? AND used_at IS NULL",
                Integer.class,
                account.user().getId()
        );
        assertThat(attempts).isEqualTo(5);
    }

    @Test
    void emailChangeRejectsCurrentOrRegisteredAddress() throws Exception {
        TestAccount account = createAccount("email-conflict", "Current Browser");
        TestAccount registered = createAccount("email-registered", "Other Browser");

        assertEmailChangeRejected(account, account.user().getEmail(), 400,
                "New email must be different from the current email");
        assertEmailChangeRejected(account, registered.user().getEmail().toUpperCase(), 409,
                "Email is already registered");
    }

    private TestAccount createAccount(String suffix, String userAgent) {
        AppUser user = userService.createUser(
                "Settings",
                "Tester",
                suffix + "@example.com",
                passwordEncoder.encode(PASSWORD),
                "+1415" + PHONE_SEQUENCE.incrementAndGet(),
                LocalDate.of(2000, 1, 1),
                "San Francisco",
                "California",
                "USA"
        );
        user.markEmailVerified(Instant.now());
        userService.save(user);
        IssuedSession session = sessionService.create(user, userAgent);
        String accessToken = jwtService.generateToken(user, session.sessionId());
        return new TestAccount(user, session, "Bearer " + accessToken);
    }

    private void requestEmailChange(TestAccount account, String email) throws Exception {
        mockMvc.perform(post("/api/users/me/email-change")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"%s","currentPassword":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isNoContent());
    }

    private void assertEmailChangeRejected(
            TestAccount account,
            String email,
            int statusCode,
            String message
    ) throws Exception {
        mockMvc.perform(post("/api/users/me/email-change")
                        .header(HttpHeaders.AUTHORIZATION, account.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail":"%s","currentPassword":"%s"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.message").value(message));
    }

    private String latestEmailChangeCode(String email) {
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailService, atLeastOnce()).sendEmailChangeCode(eq(email), code.capture());
        return code.getAllValues().getLast();
    }

    private Cookie refreshCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("open_circle_refresh");
        assertThat(cookie).isNotNull();
        return cookie;
    }

    private void assertRefreshAccepted(Cookie cookie) throws Exception {
        mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    private void assertRefreshRejected(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("open_circle_refresh", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    private int activeSessionCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auth_sessions WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private int unusedPasswordResetCodeCount(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM password_reset_codes WHERE user_id = ? AND used_at IS NULL",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private record TestAccount(AppUser user, IssuedSession session, String authorization) {
    }
}
