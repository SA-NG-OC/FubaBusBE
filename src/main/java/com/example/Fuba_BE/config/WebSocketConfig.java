package com.example.Fuba_BE.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time seat locking functionality.
 * Uses STOMP protocol over SockJS for browser compatibility.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure the message broker.
     * - /topic: For broadcasting messages to all subscribers (pub/sub)
     * - /queue: For point-to-point messaging (private messages)
     * - /app: Prefix for messages bound for @MessageMapping methods
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for topics and queues
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages from clients to server (handled by @MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints.
     * Clients connect to these endpoints to establish WebSocket connections.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure appropriately for production
                .withSockJS();
        
        // Alternative endpoint without SockJS (for native WebSocket clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
