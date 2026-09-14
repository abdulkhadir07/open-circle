package com.opencircle.notification;

public interface NotificationPublisher {

    void publish(NotificationCommand command);
}
