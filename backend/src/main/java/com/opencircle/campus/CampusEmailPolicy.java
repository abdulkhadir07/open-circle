package com.opencircle.campus;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class CampusEmailPolicy {

    private final CampusProperties properties;

    CampusEmailPolicy(CampusProperties properties) {
        this.properties = properties;
    }

    // Rejects any email that is not from a campus domain; a blank configured suffix allows everything.
    public void requireCampusEmail(String email) {
        String suffix = properties.getRequiredEmailSuffix();

        if (suffix == null || suffix.isBlank()) {
            return;
        }

        String normalizedSuffix = suffix.trim().toLowerCase(Locale.ROOT);
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.endsWith(normalizedSuffix)) {
            throw new InvalidCampusEmailException(normalizedSuffix);
        }
    }
}
