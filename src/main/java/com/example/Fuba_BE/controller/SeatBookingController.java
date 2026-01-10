package com.example.Fuba_BE.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.seat.SeatBookingConfirmRequest;
import com.example.Fuba_BE.dto.seat.SeatStatusMessage;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.ISeatLockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller for seat booking confirmation.
 * Called after successful payment to finalize the booking.
 */
@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@Slf4j
public class SeatBookingController {
    
    private final ISeatLockService seatLockService;
    private final SimpMessagingTemplate messagingTemplate;
    
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
        
        log.info("Confirming booking - seatId: {}, tripId: {}, userId: {}, paymentId: {}", 
                request.getSeatId(), request.getTripId(), request.getUserId(), request.getPaymentId());
        
        // Validate request
        if (request.getSeatId() == null || request.getTripId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("seatId, tripId, and userId are required", "BAD_REQUEST")
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
            log.info("Booking confirmed and broadcasted to {}", destination);
            
            return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", result));
        } else {
            log.warn("Booking failed: {}", result.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(result.getMessage(), result, "BOOKING_FAILED")
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
        
        log.info("REST unlock request - seatId: {}, tripId: {}, userId: {}", seatId, tripId, userId);
        
        // Use empty session ID for REST requests
        SeatStatusMessage result = seatLockService.unlockSeat(seatId, tripId, userId, "");
        
        if (result.isSuccess()) {
            // Broadcast to all subscribers
            String destination = seatLockService.getTripTopic(tripId);
            messagingTemplate.convertAndSend(destination, result);
            
            return ResponseEntity.ok(
                    ApiResponse.success("Lock released successfully", result)
            );
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(result.getMessage(), result, "LOCK_RELEASE_FAILED")
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
        
        log.info("REST lock request - seatId: {}, tripId: {}, userId: {}", 
                seatId, request.getTripId(), request.getUserId());
        
        if (request.getTripId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("tripId and userId are required", "BAD_REQUEST")
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
                    ApiResponse.success("Seat locked successfully", result)
            );
        } else {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(result.getMessage(), result, "LOCK_FAILED")
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
