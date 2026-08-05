package com.opencircle.verification;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.verification.email")

public class EmailVerificationProperties {

    @Min(1)
    @Max(60)
    private int codeExpirationMinutes;

    @Min(1)
    @Max(10)
    private int maxAttempts;

    public int getCodeExpirationMinutes() {
        return codeExpirationMinutes;
    }

    public void setCodeExpirationMinutes(int codeExpirationMinutes) {
        this.codeExpirationMinutes = codeExpirationMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
