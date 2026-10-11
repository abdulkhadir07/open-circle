package com.opencircle.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    SmtpMailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(outboundMail.recipient());
        message.setSubject(outboundMail.subject());
        message.setText(outboundMail.text());

        mailSender.send(message);
    }
}
