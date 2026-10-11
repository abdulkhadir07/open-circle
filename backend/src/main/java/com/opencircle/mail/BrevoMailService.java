package com.opencircle.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.util.List;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
class BrevoMailService implements MailService {

    private static final String API_KEY_HEADER = "api-key";

    private final RestClient restClient;
    private final MailProperties mailProperties;

    @Autowired
    BrevoMailService(MailProperties mailProperties) {
        this(newBuilder(mailProperties), mailProperties);
    }

    BrevoMailService(RestClient.Builder builder, MailProperties mailProperties) {
        this.mailProperties = mailProperties;

        MailProperties.Brevo brevo = mailProperties.getBrevo();
        String apiKey = requireConfigured(brevo.getApiKey(), "BREVO_API_KEY");
        String baseUrl = requireConfigured(brevo.getBaseUrl(), "app.mail.brevo.base-url");

        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(API_KEY_HEADER, apiKey)
                .build();
    }

    private static RestClient.Builder newBuilder(MailProperties mailProperties) {
        MailProperties.Brevo brevo = mailProperties.getBrevo();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(brevo.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(brevo.getReadTimeout());
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Override
    public void sendEmailVerificationCode(String to, String code) {
        send(OpenCircleMailMessages.emailVerification(to, code));
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        send(OpenCircleMailMessages.passwordReset(to, code));
    }

    @Override
    public void sendEmailChangeCode(String to, String code) {
        send(OpenCircleMailMessages.emailChange(to, code));
    }

    @Override
    public void sendEmailChangedNotice(String oldEmail, String newEmail) {
        send(OpenCircleMailMessages.emailChangedNotice(oldEmail, newEmail));
    }

    private void send(OutboundMail outboundMail) {
        BrevoEmailRequest request = new BrevoEmailRequest(
                new BrevoSender(mailProperties.getFromName(), mailProperties.getFrom()),
                List.of(new BrevoRecipient(outboundMail.recipient())),
                outboundMail.subject(),
                outboundMail.text()
        );

        try {
            restClient.post()
                    .uri("/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new MailDeliveryException(exception);
        }
    }

    private static String requireConfigured(String value, String setting) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(setting + " must be configured when app.mail.provider=brevo");
        }
        return value;
    }

    private record BrevoEmailRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            String subject,
            String textContent
    ) {
    }

    private record BrevoSender(String name, String email) {
    }

    private record BrevoRecipient(String email) {
    }
}
