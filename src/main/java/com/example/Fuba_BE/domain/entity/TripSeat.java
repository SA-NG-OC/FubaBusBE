package com.example.Fuba_BE.domain.entity;

import java.time.LocalDateTime;

import com.example.Fuba_BE.domain.enums.SeatStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tripseats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seatid")
    private Integer seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false)
    private Trip trip;

    @Column(name = "seatnumber", nullable = false)
    private String seatNumber;

    @Column(name = "floornumber")
    @Builder.Default
    private Integer floorNumber = 1;

    @Column(name = "seattype")
    @Builder.Default
    private String seatType = "Standard";

    @Column(name = "status")
    @Builder.Default
    private String status = "Available";

    @Column(name = "holdexpiry")
    private LocalDateTime holdExpiry;

    @Column(name = "lockedby")
    private String lockedBy;

    @Column(name = "lockedbysessionid")
    private String lockedBySessionId;


    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }


    /**
     * Helper method to check if the seat is available for locking
     */
    public boolean isAvailable() {
        return SeatStatus.AVAILABLE.getDisplayName().equals(this.status);
    }

    /**
     * Helper method to check if the seat is currently locked
     */
    public boolean isLocked() {
        return SeatStatus.LOCKED.getDisplayName().equals(this.status);
    }

    /**
     * Helper method to check if the seat is booked
     */
    public boolean isBooked() {
        return SeatStatus.BOOKED.getDisplayName().equals(this.status);
    }

    /**
     * Helper method to check if the lock has expired
     */
    public boolean isLockExpired() {
        return holdExpiry != null && LocalDateTime.now().isAfter(holdExpiry);
    }

    /**
     * Lock this seat for a specific user and session
     */
    public void lock(String userId, String sessionId, int lockDurationMinutes) {
        this.status = SeatStatus.LOCKED.getDisplayName();
        this.lockedBy = userId;
        this.lockedBySessionId = sessionId;
        this.holdExpiry = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }

    /**
     * Release the lock on this seat
     */
    public void release() {
        this.status = SeatStatus.AVAILABLE.getDisplayName();
        this.lockedBy = null;
        this.lockedBySessionId = null;
        this.holdExpiry = null;
    }

    /**
     * Mark this seat as booked
     */
    public void book() {
        this.status = SeatStatus.BOOKED.getDisplayName();
        this.holdExpiry = null;
        // Keep lockedBy as the booking user reference
    }

}