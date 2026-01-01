package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.seat.SeatBookingConfirmRequest;
import com.example.Fuba_BE.dto.seat.SeatStatusMessage;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for seat booking confirmation.
 * Called after successful payment to finalize the booking.
 */
@RestController
@RequestMapping("/api/seats")
public class SeatBookingController {
    
    private static final Logger logger = LoggerFactory.getLogger(SeatBookingController.class);
    
    private final SeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;
    
    public SeatBookingController(SeatLockService seatLockService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.seatLockService = seatLockService;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Confirm seat booking after successful payment.
     * This endpoint should be called by the payment service after payment confirmation.
     * 
     * POST /api/seats/confirm-booking
     * 
     * Request body:
     * {
     *   "seatId": 123,
     *   "tripId": 456,
     *   "userId": "user_abc",
     *   "paymentId": "pay_xyz" (optional)
     * }
     * 
     * @param request The booking confirmation request
     * @return ApiResponse with booking result
     */
    @PostMapping("/confirm-booking")
    public ResponseEntity<ApiResponse<SeatStatusMessage>> confirmBooking(
            @RequestBody SeatBookingConfirmRequest request) {
        
        logger.info("Confirming booking - seatId: {}, tripId: {}, userId: {}, paymentId: {}", 
                request.getSeatId(), request.getTripId(), request.getUserId(), request.getPaymentId());
        
        // Validate request
        if (request.getSeatId() == null || request.getTripId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "seatId, tripId, and userId are required", null)
            );
        }
        
        // Confirm the booking
        SeatStatusMessage result = seatLockService.confirmBooking(
                request.getSeatId(),
                request.getTripId(),
                request.getUserId()
        );
        
        if (result.isSuccess()) {
            // Broadcast to all subscribers
            String destination = seatLockService.getTripTopic(request.getTripId());
            messagingTemplate.convertAndSend(destination, result);
            logger.info("Booking confirmed and broadcasted to {}", destination);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Booking confirmed successfully", result)
            );
        } else {
            logger.warn("Booking failed: {}", result.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, result.getMessage(), result)
            );
        }
    }
    
    /**
     * Manually release a seat lock (cancel selection).
     * Alternative REST endpoint for clients that can't use WebSocket.
     * 
     * DELETE /api/seats/{seatId}/lock?tripId={tripId}&userId={userId}
     * 
     * @param seatId The seat ID to unlock
     * @param tripId The trip ID
     * @param userId The user ID (must own the lock)
     * @return ApiResponse with unlock result
     */
    @DeleteMapping("/{seatId}/lock")
    public ResponseEntity<ApiResponse<SeatStatusMessage>> releaseLock(
            @PathVariable Integer seatId,
            @RequestParam Integer tripId,
            @RequestParam String userId) {
        
        logger.info("REST unlock request - seatId: {}, tripId: {}, userId: {}", seatId, tripId, userId);
        
        // Use empty session ID for REST requests
        SeatStatusMessage result = seatLockService.unlockSeat(seatId, tripId, userId, "");
        
        if (result.isSuccess()) {
            // Broadcast to all subscribers
            String destination = seatLockService.getTripTopic(tripId);
            messagingTemplate.convertAndSend(destination, result);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Lock released successfully", result)
            );
        } else {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, result.getMessage(), result)
            );
        }
    }
    
    /**
     * REST endpoint to lock a seat (alternative to WebSocket).
     * 
     * POST /api/seats/{seatId}/lock
     * 
     * Request body:
     * {
     *   "tripId": 456,
     *   "userId": "user_abc"
     * }
     */
    @PostMapping("/{seatId}/lock")
    public ResponseEntity<ApiResponse<SeatStatusMessage>> lockSeat(
            @PathVariable Integer seatId,
            @RequestBody LockRequest request) {
        
        logger.info("REST lock request - seatId: {}, tripId: {}, userId: {}", 
                seatId, request.getTripId(), request.getUserId());
        
        if (request.getTripId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "tripId and userId are required", null)
            );
        }
        
        // Use userId as session ID for REST requests
        SeatStatusMessage result = seatLockService.lockSeat(
                seatId, request.getTripId(), request.getUserId(), request.getUserId());
        
        if (result.isSuccess()) {
            // Broadcast to all subscribers
            String destination = seatLockService.getTripTopic(request.getTripId());
            messagingTemplate.convertAndSend(destination, result);
            
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Seat locked successfully", result)
            );
        } else {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, result.getMessage(), result)
            );
        }
    }
    
    /**
     * Simple lock request body for REST API
     */
    public static class LockRequest {
        private Integer tripId;
        private String userId;
        
        public Integer getTripId() {
            return tripId;
        }
        
        public void setTripId(Integer tripId) {
            this.tripId = tripId;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
