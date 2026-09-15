package com.opencircle.notification;

public interface AggregatedNotificationPublisher {

    void publishAggregated(NotificationCommand command);
}
