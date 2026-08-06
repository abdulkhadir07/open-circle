package com.opencircle.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
class SmtpMailService implements MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    SmtpMailService(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendEmailVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("Verify your OpenCircle account");

        // Intentionally kept the email plain text so it works consistently across local Mailpit and real SMTP providers.
        message.setText("""
                Welcome to OpenCircle!

                Your verification code is: %s

                This code will expire soon. If you did not create an OpenCircle account, you can ignore this email.
                """.formatted(code));

        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFrom());
        message.setTo(to);
        message.setSubject("Reset your OpenCircle password");

        // Kept reset email text-only so local Mailpit and future SMTP providers behave the same way.
        message.setText("""
            We received a request to reset your OpenCircle password.

            Your password reset code is: %s

            This code will expire soon. If you did not request a password reset, you can ignore this email.
            """.formatted(code));

        mailSender.send(message);
    }
}
