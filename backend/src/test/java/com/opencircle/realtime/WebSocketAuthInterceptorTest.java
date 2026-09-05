package com.opencircle.realtime;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwtDecoder);

    @Test
    void connectWithValidBearerTokenSetsWebSocketPrincipal() {
        UUID userId = UUID.randomUUID();
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt(userId));

        Message<?> result = interceptor.preSend(stompMessage(StompCommand.CONNECT, "Bearer valid-token"), channel);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isInstanceOf(WebSocketUserPrincipal.class);
        assertThat(((WebSocketUserPrincipal) accessor.getUser()).userId()).isEqualTo(userId);
    }

    @Test
    void connectWithoutBearerTokenThrowsAuthenticationRequired() {
        Message<?> message = stompMessage(StompCommand.CONNECT, null);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Authentication required");
    }

    @Test
    void connectWithInvalidTokenThrowsInvalidAuthenticationToken() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("bad token"));

        Message<?> message = stompMessage(StompCommand.CONNECT, "Bearer bad-token");

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(MessagingException.class)
                .hasMessage("Invalid authentication token");
    }

    @Test
    void nonConnectFramePassesThroughWithoutAuthentication() {
        Message<?> message = stompMessage(StompCommand.SEND, null);

        assertThat(interceptor.preSend(message, channel)).isSameAs(message);

        verifyNoInteractions(jwtDecoder);
    }

    private Message<byte[]> stompMessage(StompCommand command, String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);

        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }

        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt(UUID userId) {
        return new Jwt(
                "valid-token",
                NOW,
                NOW.plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString())
        );
    }
}