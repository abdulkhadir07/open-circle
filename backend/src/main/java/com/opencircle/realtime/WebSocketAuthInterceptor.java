package com.opencircle.realtime;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    WebSocketAuthInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        // Authenticates the STOMP session from the JWT sent in the CONNECT frame.
        accessor.setUser(authenticate(accessor));

        return message;
    }

    private WebSocketUserPrincipal authenticate(StompHeaderAccessor accessor) {
        String token = bearerToken(accessor);

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String subject = jwt.getSubject();

            if (subject == null || subject.isBlank()) {
                throw new MessagingException("Invalid authentication token");
            }

            return new WebSocketUserPrincipal(UUID.fromString(subject));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new MessagingException("Invalid authentication token", exception);
        }
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);

        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            throw new MessagingException("Authentication required");
        }

        String authorization = authorizationHeaders.getFirst();

        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Authentication required");
        }

        return authorization.substring(BEARER_PREFIX.length());
    }
}