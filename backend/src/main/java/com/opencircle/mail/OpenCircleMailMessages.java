package com.opencircle.mail;

final class OpenCircleMailMessages {

    private OpenCircleMailMessages() {
    }

    static OutboundMail emailVerification(String recipient, String code) {
        return new OutboundMail(
                recipient,
                "Verify your OpenCircle account",
                """
                        Welcome to OpenCircle!

                        Your verification code is: %s

                        This code will expire soon. If you did not create an OpenCircle account, you can ignore this email.
                        """.formatted(code)
        );
    }

    static OutboundMail passwordReset(String recipient, String code) {
        return new OutboundMail(
                recipient,
                "Reset your OpenCircle password",
                """
                        We received a request to reset your OpenCircle password.

                        Your password reset code is: %s

                        This code will expire soon. If you did not request a password reset, you can ignore this email.
                        """.formatted(code)
        );
    }

    static OutboundMail emailChange(String recipient, String code) {
        return new OutboundMail(
                recipient,
                "Verify your new OpenCircle email address",
                """
                        We received a request to use this email address for an OpenCircle account.

                        Your email change code is: %s

                        This code will expire soon. If you did not request this change, you can ignore this email.
                        """.formatted(code)
        );
    }

    static OutboundMail emailChangedNotice(String oldEmail, String newEmail) {
        return new OutboundMail(
                oldEmail,
                "Your OpenCircle email address was changed",
                """
                        The email address on your OpenCircle account was changed to %s.

                        If you did not make this change, reset your password immediately.
                        """.formatted(newEmail)
        );
    }
}
