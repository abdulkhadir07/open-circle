package com.opencircle.mail;

public interface MailService {

    void sendEmailVerificationCode(String to, String code);
}
