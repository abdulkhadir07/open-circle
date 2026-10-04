package com.opencircle.campus;

import java.util.Locale;

/**
 * A campus is identified by the registrable domain of a member's email address
 * (student.sfsu.edu and mail.sfsu.edu both belong to sfsu.edu).
 */
public final class Campus {

    private Campus() {
    }

    public static String fromEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }

        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            throw new IllegalArgumentException("Email must contain a domain");
        }

        String domain = email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
        String[] labels = domain.split("\\.");

        if (labels.length <= 2) {
            return domain;
        }

        return labels[labels.length - 2] + "." + labels[labels.length - 1];
    }
}
