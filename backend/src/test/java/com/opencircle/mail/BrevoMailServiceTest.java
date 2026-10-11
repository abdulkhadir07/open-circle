package com.opencircle.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoMailServiceTest {

    private static final String BASE_URL = "https://brevo.test";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void sendsVerificationEmailThroughTheTransactionalApi() {
        expectEmail(
                "jane@example.com",
                "Verify your OpenCircle account",
                "Your verification code is: 123456"
        );

        service(properties("secret-api-key"))
                .sendEmailVerificationCode("jane@example.com", "123456");

        server.verify();
    }

    @Test
    void sendsPasswordResetEmailThroughTheTransactionalApi() {
        expectEmail(
                "jane@example.com",
                "Reset your OpenCircle password",
                "Your password reset code is: 654321"
        );

        service(properties("secret-api-key"))
                .sendPasswordResetCode("jane@example.com", "654321");

        server.verify();
    }

    @Test
    void sendsEmailChangeCodeToTheNewAddress() {
        expectEmail(
                "new@example.com",
                "Verify your new OpenCircle email address",
                "Your email change code is: 123456"
        );

        service(properties("secret-api-key"))
                .sendEmailChangeCode("new@example.com", "123456");

        server.verify();
    }

    @Test
    void sendsEmailChangedNoticeToTheOldAddress() {
        expectEmail(
                "old@example.com",
                "Your OpenCircle email address was changed",
                "changed to new@example.com"
        );

        service(properties("secret-api-key"))
                .sendEmailChangedNotice("old@example.com", "new@example.com");

        server.verify();
    }

    @Test
    void failsFastWhenTheApiKeyIsMissing() {
        MailProperties properties = properties(" ");

        assertThatThrownBy(() -> service(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("BREVO_API_KEY must be configured when app.mail.provider=brevo");
    }

    @Test
    void wrapsBrevoApiFailuresWithoutExposingTheMessageContents() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service(properties("secret-api-key"))
                .sendEmailVerificationCode("jane@example.com", "123456"))
                .isInstanceOf(MailDeliveryException.class)
                .hasMessage("Unable to send email")
                .hasMessageNotContaining("123456")
                .hasMessageNotContaining("secret-api-key");

        server.verify();
    }

    private void expectEmail(String recipient, String subject, String textFragment) {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "secret-api-key"))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.name").value("OpenCircle"))
                .andExpect(jsonPath("$.sender.email").value("no-reply@opencircle.test"))
                .andExpect(jsonPath("$.to[0].email").value(recipient))
                .andExpect(jsonPath("$.subject").value(subject))
                .andExpect(jsonPath("$.textContent").value(org.hamcrest.Matchers.containsString(textFragment)))
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"message-id\"}"));
    }

    private BrevoMailService service(MailProperties properties) {
        return new BrevoMailService(builder, properties);
    }

    private MailProperties properties(String apiKey) {
        MailProperties properties = new MailProperties();
        properties.setFrom("no-reply@opencircle.test");
        properties.setFromName("OpenCircle");
        properties.getBrevo().setApiKey(apiKey);
        properties.getBrevo().setBaseUrl(BASE_URL);
        return properties;
    }
}
