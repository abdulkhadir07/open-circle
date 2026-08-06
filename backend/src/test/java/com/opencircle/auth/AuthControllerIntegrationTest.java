package com.opencircle.auth;

import com.opencircle.AbstractIntegrationTest;
import com.opencircle.mail.MailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MailService mailService;

    @Test
    void signupCreatesUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Jane",
                                  "lastName": "Doe",
                                  "email": "jane.controller@example.com",
                                  "password": "Password123!",
                                  "phoneNumber": "+14155550199",
                                  "dateOfBirth": "2000-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("jane.controller@example.com"))
                .andExpect(jsonPath("$.user.emailVerified").value(false))
                .andExpect(jsonPath("$.user.username", notNullValue()));

        verify(mailService).sendEmailVerificationCode(
                org.mockito.Mockito.eq("jane.controller@example.com"),
                org.mockito.Mockito.anyString()
        );
    }

    @Test
    void loginRejectsUnverifiedUser() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "john.controller@example.com",
                                  "password": "Password123!",
                                  "phoneNumber": "+14155550200",
                                  "dateOfBirth": "2000-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "john.controller@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Please verify your email before logging in"));
    }

    @Test
    void signupRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Doe",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "phoneNumber": "",
                                  "dateOfBirth": "2030-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void signupVerifyEmailThenLoginReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Amina",
                                  "lastName": "Sowe",
                                  "email": "amina.controller@example.com",
                                  "password": "Password123!",
                                  "phoneNumber": "+14155550210",
                                  "dateOfBirth": "2000-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """))
                .andExpect(status().isCreated());

        String code = latestSentCode();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "amina.controller@example.com",
                                  "code": "%s"
                                }
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.email").value("amina.controller@example.com"))
                .andExpect(jsonPath("$.user.emailVerified").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "amina.controller@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.emailVerified").value(true));
    }

    @Test
    void resendVerificationSendsAnotherCode() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Musa",
                                  "lastName": "Bah",
                                  "email": "musa.controller@example.com",
                                  "password": "Password123!",
                                  "phoneNumber": "+14155550211",
                                  "dateOfBirth": "2000-01-01",
                                  "city": "San Francisco",
                                  "stateRegion": "California",
                                  "country": "USA"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "musa.controller@example.com"
                                }
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService, times(2)).sendEmailVerificationCode(emailCaptor.capture(), codeCaptor.capture());

        List<String> emails = emailCaptor.getAllValues();
        List<String> codes = codeCaptor.getAllValues();

        org.assertj.core.api.Assertions.assertThat(emails)
                .containsExactly("musa.controller@example.com", "musa.controller@example.com");

        org.assertj.core.api.Assertions.assertThat(codes)
                .allMatch(code -> code.matches("\\d{6}"));
    }

    @Test
    void verifyEmailRejectsInvalidCode() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "firstName": "Fatou",
                              "lastName": "Jallow",
                              "email": "fatou.controller@example.com",
                              "password": "Password123!",
                              "phoneNumber": "+14155550212",
                              "dateOfBirth": "2000-01-01",
                              "city": "San Francisco",
                              "stateRegion": "California",
                              "country": "USA"
                            }
                            """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "fatou.controller@example.com",
                              "code": "000000"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification code"));
    }

    private String latestSentCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService).sendEmailVerificationCode(
                org.mockito.Mockito.anyString(),
                codeCaptor.capture()
        );

        return codeCaptor.getValue();
    }

    @Test
    void forgotPasswordReturnsNoContentForUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "unknown.controller@example.com"
                            }
                            """))
                .andExpect(status().isNoContent());
    }

    @Test
    void forgotPasswordSendsResetCodeForExistingUser() throws Exception {
        createVerifiedUser(
                "reset.controller@example.com",
                "+14155550220"
        );

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "reset.controller@example.com"
                            }
                            """))
                .andExpect(status().isNoContent());

        verify(mailService).sendPasswordResetCode(
                org.mockito.Mockito.eq("reset.controller@example.com"),
                org.mockito.Mockito.anyString()
        );
    }

    @Test
    void forgotPasswordDoesNotRevealUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "nobody.controller@example.com"
                            }
                            """))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(mailService, org.mockito.Mockito.never())
                .sendPasswordResetCode(
                        org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.anyString()
                );
    }

    @Test
    void resetPasswordChangesPasswordAndAllowsLogin() throws Exception {
        createVerifiedUser(
                "changepassword.controller@example.com",
                "+14155550221"
        );

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "changepassword.controller@example.com"
                            }
                            """))
                .andExpect(status().isNoContent());

        String resetCode = latestPasswordResetCode();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "changepassword.controller@example.com",
                              "code": "%s",
                              "newPassword": "NewPassword123!"
                            }
                            """.formatted(resetCode)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "changepassword.controller@example.com",
                              "password": "NewPassword123!"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()));
    }

    @Test
    void resetPasswordRejectsInvalidCode() throws Exception {
        createVerifiedUser(
                "badreset.controller@example.com",
                "+14155550222"
        );

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "badreset.controller@example.com"
                            }
                            """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "badreset.controller@example.com",
                              "code": "000000",
                              "newPassword": "NewPassword123!"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired password reset code"));
    }

    @Test
    void resetPasswordRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "not-an-email",
                              "code": "abc",
                              "newPassword": "short"
                            }
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    private void createVerifiedUser(String email, String phoneNumber) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "firstName": "Reset",
                              "lastName": "User",
                              "email": "%s",
                              "password": "Password123!",
                              "phoneNumber": "%s",
                              "dateOfBirth": "2000-01-01",
                              "city": "San Francisco",
                              "stateRegion": "California",
                              "country": "USA"
                            }
                            """.formatted(email, phoneNumber)))
                .andExpect(status().isCreated());

        String verificationCode = latestEmailVerificationCode();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "%s",
                              "code": "%s"
                            }
                            """.formatted(email, verificationCode)))
                .andExpect(status().isOk());
    }

    private String latestEmailVerificationCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService, org.mockito.Mockito.atLeastOnce()).sendEmailVerificationCode(
                org.mockito.Mockito.anyString(),
                codeCaptor.capture()
        );

        List<String> codes = codeCaptor.getAllValues();
        return codes.getLast();
    }

    private String latestPasswordResetCode() {
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);

        verify(mailService, org.mockito.Mockito.atLeastOnce()).sendPasswordResetCode(
                org.mockito.Mockito.anyString(),
                codeCaptor.capture()
        );

        List<String> codes = codeCaptor.getAllValues();
        return codes.getLast();
    }


}
