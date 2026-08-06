package com.opencircle.mail;

public interface MailService {

    void sendEmailVerificationCode(String to, String code);
    void sendPasswordResetCode(String to, String code);
}
