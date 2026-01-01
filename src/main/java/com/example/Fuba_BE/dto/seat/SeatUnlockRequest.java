package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for unlocking a seat via WebSocket.
 * Client sends this to /app/seat/unlock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatUnlockRequest {
    /**
     * The ID of the seat to unlock
     */
    private Integer seatId;
    
    /**
     * The ID of the trip (for validation and topic routing)
     */
    private Integer tripId;
    
    /**
     * User identifier (must match the user who locked it)
     */
    private String userId;
}
