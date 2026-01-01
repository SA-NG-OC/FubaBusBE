package com.example.Fuba_BE.config;

import com.example.Fuba_BE.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event listener for WebSocket session lifecycle events.
 * Handles connection, disconnection, subscription, and unsubscription events.
 * 
 * Key functionality:
 * - Track active sessions
 * - Release seat locks when a user disconnects (page refresh, close, or network loss)
 * - Log session activity for monitoring
 */
@Component
public class WebSocketEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final SeatLockService seatLockService;
    
    // Track active sessions with their user info
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    public WebSocketEventListener(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }
    
    /**
     * Handle new WebSocket connection.
     * Called when a client successfully connects via STOMP.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        // Store session info
        activeSessions.put(sessionId, new SessionInfo(sessionId, System.currentTimeMillis()));
        
        logger.info("WebSocket session connected: {}", sessionId);
        logger.debug("Active sessions count: {}", activeSessions.size());
    }
    
    /**
     * Handle WebSocket disconnection.
     * Called when a client disconnects (close tab, refresh, network loss, etc.).
     * 
     * This is the KEY method that releases all seat locks held by the disconnecting user.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        logger.info("WebSocket session disconnected: {}", sessionId);
        
        // Release all seats locked by this session
        try {
            var releasedSeats = seatLockService.releaseAllBySession(sessionId);
            
            if (!releasedSeats.isEmpty()) {
                logger.info("Released {} seat locks for disconnected session {}", 
                        releasedSeats.size(), sessionId);
                
                // Log each released seat for debugging
                releasedSeats.forEach(seat -> 
                    logger.debug("Released seat {} on trip {}", seat.getSeatId(), seat.getTripId())
                );
            }
        } catch (Exception e) {
            logger.error("Error releasing seats for disconnected session {}: {}", 
                    sessionId, e.getMessage(), e);
        }
        
        // Remove session from tracking
        activeSessions.remove(sessionId);
        logger.debug("Active sessions count: {}", activeSessions.size());
    }
    
    /**
     * Handle topic subscription.
     * Called when a client subscribes to a destination (e.g., /topic/trips/123/seats).
     */
    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        logger.debug("Session {} subscribed to {}", sessionId, destination);
        
        // Update session info with subscription
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo != null) {
            sessionInfo.addSubscription(destination);
        }
    }
    
    /**
     * Handle topic unsubscription.
     * Called when a client unsubscribes from a destination.
     */
    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String subscriptionId = headerAccessor.getSubscriptionId();
        
        logger.debug("Session {} unsubscribed from subscription {}", sessionId, subscriptionId);
        
        // Optionally release locks when unsubscribing from a trip topic
        // This is handled more reliably by the disconnect event
    }
    
    /**
     * Get the count of active WebSocket sessions.
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Check if a session is active.
     */
    public boolean isSessionActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }
    
    /**
     * Inner class to track session information.
     */
    private static class SessionInfo {
        private final String sessionId;
        private final long connectedAt;
        private final Map<String, Long> subscriptions = new ConcurrentHashMap<>();
        
        public SessionInfo(String sessionId, long connectedAt) {
            this.sessionId = sessionId;
            this.connectedAt = connectedAt;
        }
        
        public void addSubscription(String destination) {
            subscriptions.put(destination, System.currentTimeMillis());
        }
        
        public void removeSubscription(String destination) {
            subscriptions.remove(destination);
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getConnectedAt() {
            return connectedAt;
        }
        
        public Map<String, Long> getSubscriptions() {
            return subscriptions;
        }
    }
}
