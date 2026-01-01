package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for confirming seat booking after payment.
 * Used by REST endpoint POST /api/seats/confirm-booking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatBookingConfirmRequest {
    /**
     * The ID of the seat to confirm booking
     */
    private Integer seatId;
    
    /**
     * The ID of the trip
     */
    private Integer tripId;
    
    /**
     * User who is booking the seat (must match the lock holder)
     */
    private String userId;
    
    /**
     * Payment reference ID (optional, for audit)
     */
    private String paymentId;
}
