package com.opencircle.notification;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    @Min(1)
    private long retentionDays = 90;

    public long getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(long retentionDays) {
        this.retentionDays = retentionDays;
    }
}
