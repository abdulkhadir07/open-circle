package com.opencircle.realtime;

import com.opencircle.security.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final ChatSubscriptionInterceptor chatSubscriptionInterceptor;

    WebSocketConfig(
            CorsProperties corsProperties,
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            ChatSubscriptionInterceptor chatSubscriptionInterceptor
    ) {
        this.corsProperties = corsProperties;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.chatSubscriptionInterceptor = chatSubscriptionInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Routes client-sent messages through @MessageMapping handlers under /app.
        registry.setApplicationDestinationPrefixes("/app");

        // Publishes room updates and user-specific errors through the in-memory broker.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Reuses the same allowed frontend origins as the REST API.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Authenticates the session first, then authorizes room-specific subscriptions.
        registration.interceptors(webSocketAuthInterceptor, chatSubscriptionInterceptor);
    }
}