package com.example.Fuba_BE.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.example.Fuba_BE.dto.seat.SeatLockRequest;
import com.example.Fuba_BE.dto.seat.SeatStatusMessage;
import com.example.Fuba_BE.dto.seat.SeatUnlockRequest;
import com.example.Fuba_BE.service.ISeatLockService;

/**
 * WebSocket Controller for real-time seat locking operations.
 * Handles STOMP messages for seat lock/unlock requests.
 * 
 * Message Mapping:
 * - /app/seat/lock    -> lockSeat()    -> broadcasts to /topic/trips/{tripId}/seats
 * - /app/seat/unlock  -> unlockSeat()  -> broadcasts to /topic/trips/{tripId}/seats
 */
@Controller
public class SeatLockWebSocketController {
    
    private static final Logger logger = LoggerFactory.getLogger(SeatLockWebSocketController.class);
    
    private final ISeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public SeatLockWebSocketController(ISeatLockService seatLockService,
                                       SimpMessagingTemplate messagingTemplate) {
        this.seatLockService = seatLockService;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Handle seat lock request from client.
     * 
     * Client sends to: /app/seat/lock
     * Server broadcasts to: /topic/trips/{tripId}/seats
     * 
     * Example client message:
     * {
     *   "seatId": 123,
     *   "tripId": 456,
     *   "userId": "user_abc"
     * }
     * 
     * @param request The seat lock request payload
     * @param headerAccessor Access to STOMP headers including session ID
     * @return SeatStatusMessage result (sent to user's private queue on failure)
     */
    @MessageMapping("/seat/lock")
    @SendToUser("/queue/seat/response")
    public SeatStatusMessage lockSeat(@Payload SeatLockRequest request,
                                      SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Received lock request - seatId: {}, tripId: {}, userId: {}, sessionId: {}", 
                request.getSeatId(), request.getTripId(), request.getUserId(), sessionId);
        
        // Validate request
        if (request.getSeatId() == null || request.getTripId() == null) {
            return SeatStatusMessage.lockFailed(request.getSeatId(), request.getTripId(), 
                    "seatId and tripId are required");
        }
        
        // Use session ID as user ID if not provided (for guest users)
        String userId = request.getUserId() != null ? request.getUserId() : sessionId;
        
        // Attempt to lock the seat
        SeatStatusMessage result = seatLockService.lockSeat(
                request.getSeatId(), 
                request.getTripId(), 
                userId, 
                sessionId
        );
        
        // Broadcast to all subscribers of this trip's topic
        if (result.isSuccess()) {
            String destination = seatLockService.getTripTopic(request.getTripId());
            messagingTemplate.convertAndSend(destination, result);
            logger.info("Broadcasted lock success to {}", destination);
        }
        
        // Also return to the requesting user (for confirmation or error)
        return result;
    }
    
    /**
     * Handle seat unlock request from client.
     * 
     * Client sends to: /app/seat/unlock
     * Server broadcasts to: /topic/trips/{tripId}/seats
     * 
     * Example client message:
     * {
     *   "seatId": 123,
     *   "tripId": 456,
     *   "userId": "user_abc"
     * }
     * 
     * @param request The seat unlock request payload
     * @param headerAccessor Access to STOMP headers including session ID
     * @return SeatStatusMessage result
     */
    @MessageMapping("/seat/unlock")
    @SendToUser("/queue/seat/response")
    public SeatStatusMessage unlockSeat(@Payload SeatUnlockRequest request,
                                        SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Received unlock request - seatId: {}, tripId: {}, userId: {}, sessionId: {}", 
                request.getSeatId(), request.getTripId(), request.getUserId(), sessionId);
        
        // Validate request
        if (request.getSeatId() == null || request.getTripId() == null) {
            return SeatStatusMessage.unlockFailed(request.getSeatId(), request.getTripId(), 
                    "seatId and tripId are required");
        }
        
        // Use session ID as user ID if not provided
        String userId = request.getUserId() != null ? request.getUserId() : sessionId;
        
        // Attempt to unlock the seat
        SeatStatusMessage result = seatLockService.unlockSeat(
                request.getSeatId(), 
                request.getTripId(), 
                userId, 
                sessionId
        );
        
        // Broadcast to all subscribers of this trip's topic
        if (result.isSuccess()) {
            String destination = seatLockService.getTripTopic(request.getTripId());
            messagingTemplate.convertAndSend(destination, result);
            logger.info("Broadcasted unlock success to {}", destination);
        }
        
        // Also return to the requesting user
        return result;
    }
    
    /**
     * Handle subscription to a trip's seat updates.
     * Called when a client subscribes to /topic/trips/{tripId}/seats
     * 
     * This can be used to send initial seat status on subscription.
     */
    @MessageMapping("/seat/subscribe")
    public void subscribeTripSeats(@Payload SubscribeRequest request,
                                   SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Client {} subscribed to trip {} seats", sessionId, request.getTripId());
        
        // Optionally send current seat status to the user
        // This is handled by the REST API in SeatMapController
    }
    
    /**
     * Simple request for subscription
     */
    public static class SubscribeRequest {
        private Integer tripId;
        
        public Integer getTripId() {
            return tripId;
        }
        
        public void setTripId(Integer tripId) {
            this.tripId = tripId;
        }
    }
}
