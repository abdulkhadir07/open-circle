package com.opencircle.mail;

public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(Throwable cause) {
        super("Unable to send email", cause);
    }
}
