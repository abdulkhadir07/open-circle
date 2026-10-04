package com.opencircle.campus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.campus")
public class CampusProperties {

    /**
     * Email suffix a new account (or a changed email) must end with, such as ".edu".
     * Leave empty to accept any email address.
     */
    private String requiredEmailSuffix = ".edu";

    public String getRequiredEmailSuffix() {
        return requiredEmailSuffix;
    }

    public void setRequiredEmailSuffix(String requiredEmailSuffix) {
        this.requiredEmailSuffix = requiredEmailSuffix;
    }
}
