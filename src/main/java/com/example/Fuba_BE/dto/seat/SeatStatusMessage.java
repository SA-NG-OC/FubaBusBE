package com.example.Fuba_BE.dto.seat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response/Broadcast payload for seat status changes via WebSocket.
 * Server broadcasts this to /topic/trips/{tripId}/seats
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatStatusMessage {
    /**
     * Type of the message for client-side handling
     */
    private MessageType type;
    
    /**
     * The ID of the seat
     */
    private Integer seatId;
    
    /**
     * The seat number (e.g., "A1", "B2")
     */
    private String seatNumber;
    
    /**
     * The trip ID this seat belongs to
     */
    private Integer tripId;
    
    /**
     * Current status of the seat: "Available", "Held", "Booked"
     */
    private String status;
    
    /**
     * User who has locked/booked the seat (null if available)
     */
    private String lockedBy;
    
    /**
     * When the lock expires (null if not locked or if booked)
     */
    private LocalDateTime lockExpiry;
    
    /**
     * Floor number of the seat
     */
    private Integer floorNumber;
    
    /**
     * Whether the operation was successful
     */
    private boolean success;
    
    /**
     * Optional message for errors or additional info
     */
    private String message;
    
    /**
     * Timestamp of this message
     */
    private LocalDateTime timestamp;

    /**
     * Enum for message types
     */
    public enum MessageType {
        SEAT_LOCKED,
        SEAT_UNLOCKED,
        SEAT_BOOKED,
        SEAT_LOCK_FAILED,
        SEAT_UNLOCK_FAILED,
        SEAT_EXPIRED,
        SEAT_STATUS_UPDATE
    }

    /**
     * Factory method for successful lock
     */
    public static SeatStatusMessage locked(Integer seatId, String seatNumber, Integer tripId, 
                                           String lockedBy, LocalDateTime lockExpiry, Integer floorNumber) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_LOCKED)
                .seatId(seatId)
                .seatNumber(seatNumber)
                .tripId(tripId)
                .status("Held")
                .lockedBy(lockedBy)
                .lockExpiry(lockExpiry)
                .floorNumber(floorNumber)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for successful unlock
     */
    public static SeatStatusMessage unlocked(Integer seatId, String seatNumber, Integer tripId, Integer floorNumber) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_UNLOCKED)
                .seatId(seatId)
                .seatNumber(seatNumber)
                .tripId(tripId)
                .status("Available")
                .lockedBy(null)
                .lockExpiry(null)
                .floorNumber(floorNumber)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for expired lock
     */
    public static SeatStatusMessage expired(Integer seatId, String seatNumber, Integer tripId, Integer floorNumber) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_EXPIRED)
                .seatId(seatId)
                .seatNumber(seatNumber)
                .tripId(tripId)
                .status("Available")
                .lockedBy(null)
                .lockExpiry(null)
                .floorNumber(floorNumber)
                .success(true)
                .message("Lock expired")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for booked seat
     */
    public static SeatStatusMessage booked(Integer seatId, String seatNumber, Integer tripId, 
                                           String bookedBy, Integer floorNumber) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_BOOKED)
                .seatId(seatId)
                .seatNumber(seatNumber)
                .tripId(tripId)
                .status("Booked")
                .lockedBy(bookedBy)
                .lockExpiry(null)
                .floorNumber(floorNumber)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for lock failure
     */
    public static SeatStatusMessage lockFailed(Integer seatId, Integer tripId, String message) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_LOCK_FAILED)
                .seatId(seatId)
                .tripId(tripId)
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method for unlock failure
     */
    public static SeatStatusMessage unlockFailed(Integer seatId, Integer tripId, String message) {
        return SeatStatusMessage.builder()
                .type(MessageType.SEAT_UNLOCK_FAILED)
                .seatId(seatId)
                .tripId(tripId)
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
