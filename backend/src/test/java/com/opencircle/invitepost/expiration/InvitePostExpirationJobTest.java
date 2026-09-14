package com.opencircle.invitepost.expiration;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InvitePostExpirationJobTest {

    @Test
    void delegatesExpirationProcessingToService() {
        InvitePostExpirationService service = mock(InvitePostExpirationService.class);

        new InvitePostExpirationJob(service).publishExpirationNotifications();

        verify(service).processDuePosts();
    }
}
