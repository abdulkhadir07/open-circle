package com.opencircle.session;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.session")
public class SessionProperties {

    @Min(1)
    private long inactivityTimeoutDays = 7;

    @Min(1)
    private long absoluteLifetimeDays = 30;

    @Min(0)
    private long cleanupRetentionDays = 30;

    @NotBlank
    private String cookieName = "open_circle_refresh";

    @NotBlank
    private String cookiePath = "/api/auth";

    @NotBlank
    private String cookieSameSite = "Lax";

    private boolean cookieSecure = true;

    public long getInactivityTimeoutDays() {
        return inactivityTimeoutDays;
    }

    public void setInactivityTimeoutDays(long inactivityTimeoutDays) {
        this.inactivityTimeoutDays = inactivityTimeoutDays;
    }

    public long getAbsoluteLifetimeDays() {
        return absoluteLifetimeDays;
    }

    public void setAbsoluteLifetimeDays(long absoluteLifetimeDays) {
        this.absoluteLifetimeDays = absoluteLifetimeDays;
    }

    public long getCleanupRetentionDays() {
        return cleanupRetentionDays;
    }

    public void setCleanupRetentionDays(long cleanupRetentionDays) {
        this.cleanupRetentionDays = cleanupRetentionDays;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    @AssertTrue(message = "Session cookie SameSite must be Lax, Strict, or None; None also requires Secure")
    boolean isCookiePolicyValid() {
        boolean supported = "Lax".equalsIgnoreCase(cookieSameSite)
                || "Strict".equalsIgnoreCase(cookieSameSite)
                || "None".equalsIgnoreCase(cookieSameSite);
        return supported && (!"None".equalsIgnoreCase(cookieSameSite) || cookieSecure);
    }

    @AssertTrue(message = "Session inactivity timeout cannot exceed its absolute lifetime")
    boolean isLifetimePolicyValid() {
        return inactivityTimeoutDays <= absoluteLifetimeDays;
    }
}
