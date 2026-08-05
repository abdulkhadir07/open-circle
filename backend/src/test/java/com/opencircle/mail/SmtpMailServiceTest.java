package com.opencircle.mail;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpMailService {

    @Test
    void sendEmailVerificationCodeSendsPlainTextEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MailProperties properties = new MailProperties();
        properties.setFrom("no-reply@opencircle.test");

        SmtpMailService service = new SmtpMailService(mailSender, properties);

        service.sendEmailVerificationCode("jane@example.com", "123456");

        var captor = forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("no-reply@opencircle.test");
        assertThat(message.getTo()).containsExactly("jane@example.com");
        assertThat(message.getSubject()).isEqualTo("Verify your OpenCircle account");
        assertThat(message.getText()).contains("123456");
    }
}
