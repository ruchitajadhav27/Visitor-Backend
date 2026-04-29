package com.visitor.Visitor_Backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Outgoing channel
        config.setApplicationDestinationPrefixes("/app"); // Incoming channel
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("http://localhost:5173") // Use setAllowedOrigins instead of setAllowedOriginPatterns for SockJS
                .setAllowedOriginPatterns("*") // Alternatively, use patterns if you have dynamic subdomains
                .withSockJS();
    }
}
