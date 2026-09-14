package com.opencircle.notification;

import java.time.Instant;
import java.util.UUID;

interface NotificationRow {

    UUID getId();

    UUID getActorUserId();

    String getActorUsername();

    String getType();

    String getResourceType();

    UUID getResourceId();

    String getContextType();

    UUID getContextId();

    int getOccurrenceCount();

    Instant getOccurredAt();

    Instant getReadAt();
}
