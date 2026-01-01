package com.example.Fuba_BE.service;

import com.example.Fuba_BE.dto.seat.SeatStatusMessage;

import java.util.List;

/**
 * Service interface for real-time seat locking operations.
 */
public interface SeatLockService {
    
    /**
     * Lock duration in minutes (5 minutes as per requirement)
     */
    int LOCK_DURATION_MINUTES = 5;
    
    /**
     * Attempt to lock a seat for a user.
     * Uses pessimistic locking to prevent race conditions.
     * 
     * @param seatId The ID of the seat to lock
     * @param tripId The ID of the trip (for validation)
     * @param userId The user attempting to lock
     * @param sessionId The WebSocket session ID
     * @return SeatStatusMessage with result
     */
    SeatStatusMessage lockSeat(Integer seatId, Integer tripId, String userId, String sessionId);
    
    /**
     * Unlock a seat previously locked by the user.
     * 
     * @param seatId The ID of the seat to unlock
     * @param tripId The ID of the trip (for validation)
     * @param userId The user attempting to unlock (must be the lock owner)
     * @param sessionId The WebSocket session ID
     * @return SeatStatusMessage with result
     */
    SeatStatusMessage unlockSeat(Integer seatId, Integer tripId, String userId, String sessionId);
    
    /**
     * Confirm booking of a locked seat after successful payment.
     * 
     * @param seatId The ID of the seat to book
     * @param tripId The ID of the trip
     * @param userId The user confirming booking (must be the lock owner)
     * @return SeatStatusMessage with result
     */
    SeatStatusMessage confirmBooking(Integer seatId, Integer tripId, String userId);
    
    /**
     * Release all seats locked by a specific session.
     * Called when a user disconnects or refreshes the page.
     * 
     * @param sessionId The WebSocket session ID
     * @return List of SeatStatusMessage for all released seats
     */
    List<SeatStatusMessage> releaseAllBySession(String sessionId);
    
    /**
     * Release all expired seat locks.
     * Called by the scheduler.
     * 
     * @return List of SeatStatusMessage for all released seats
     */
    List<SeatStatusMessage> releaseExpiredLocks();
    
    /**
     * Get the topic destination for a specific trip.
     * 
     * @param tripId The trip ID
     * @return The topic destination string
     */
    default String getTripTopic(Integer tripId) {
        return "/topic/trips/" + tripId + "/seats";
    }
}
