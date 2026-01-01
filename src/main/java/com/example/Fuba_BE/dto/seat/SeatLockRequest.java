package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for locking a seat via WebSocket.
 * Client sends this to /app/seat/lock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLockRequest {
    /**
     * The ID of the seat to lock
     */
    private Integer seatId;
    
    /**
     * The ID of the trip (for validation and topic routing)
     */
    private Integer tripId;
    
    /**
     * User identifier (can be authenticated userId or guest sessionId)
     */
    private String userId;
}
