package com.opencircle.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealtimeChatRoomPresenceTest {

    private final SimpUserRegistry users = mock(SimpUserRegistry.class);
    private final ObjectProvider<SimpUserRegistry> userProvider = providerFor(users);
    private final RealtimeChatRoomPresence presence = new RealtimeChatRoomPresence(userProvider);

    @Test
    void absentWebSocketUserIsAway() {
        UUID userId = UUID.randomUUID();
        when(users.getUser(userId.toString())).thenReturn(null);

        assertThat(presence.isPresent(userId, UUID.randomUUID())).isFalse();
    }

    @Test
    void userIsPresentWhenAnySessionSubscribesToTheRoom() {
        UUID userId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        SimpUser user = mock(SimpUser.class);
        SimpSession firstSession = sessionWith("/topic/chat-rooms/" + UUID.randomUUID());
        SimpSession secondSession = sessionWith("/topic/chat-rooms/" + roomId);

        when(users.getUser(userId.toString())).thenReturn(user);
        when(user.getSessions()).thenReturn(Set.of(firstSession, secondSession));

        assertThat(presence.isPresent(userId, roomId)).isTrue();
    }

    @Test
    void subscriptionToAnotherRoomDoesNotCountAsPresent() {
        UUID userId = UUID.randomUUID();
        SimpUser user = mock(SimpUser.class);
        SimpSession session = sessionWith("/topic/chat-rooms/" + UUID.randomUUID());

        when(users.getUser(userId.toString())).thenReturn(user);
        when(user.getSessions()).thenReturn(Set.of(session));

        assertThat(presence.isPresent(userId, UUID.randomUUID())).isFalse();
    }

    private SimpSession sessionWith(String destination) {
        SimpSession session = mock(SimpSession.class);
        SimpSubscription subscription = mock(SimpSubscription.class);
        when(subscription.getDestination()).thenReturn(destination);
        when(session.getSubscriptions()).thenReturn(Set.of(subscription));
        return session;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<SimpUserRegistry> providerFor(SimpUserRegistry registry) {
        ObjectProvider<SimpUserRegistry> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(registry);
        return provider;
    }
}
