package com.opencircle.mail;

class MailDeliveryException extends RuntimeException {

    MailDeliveryException(Throwable cause) {
        super("Unable to send email", cause);
    }
}
