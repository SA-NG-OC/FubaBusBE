package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.TripSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripSeatRepository extends JpaRepository<TripSeat, Integer> {
    
    List<TripSeat> findByTrip_TripIdOrderByFloorNumberAscSeatNumberAsc(Integer tripId);

    boolean existsByTrip_TripId(Integer tripId);

    void deleteByTrip_TripId(Integer tripId);
    boolean existsByTrip_TripIdAndStatus(Integer tripId, String status);
    
    /**
     * Find a seat by ID with pessimistic write lock (SELECT ... FOR UPDATE).
     * This ensures no two transactions can lock the same seat simultaneously.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TripSeat ts WHERE ts.seatId = :seatId")
    Optional<TripSeat> findByIdWithLock(@Param("seatId") Integer seatId);
    
    /**
     * Find a seat by ID and trip ID with pessimistic write lock.
     * Validates that the seat belongs to the specified trip.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TripSeat ts WHERE ts.seatId = :seatId AND ts.trip.tripId = :tripId")
    Optional<TripSeat> findBySeatIdAndTripIdWithLock(@Param("seatId") Integer seatId, @Param("tripId") Integer tripId);
    
    /**
     * Find all seats locked by a specific session ID.
     * Used when a user disconnects to release their locks.
     */
    @Query("SELECT ts FROM TripSeat ts WHERE ts.lockedBySessionId = :sessionId AND ts.status = 'Held'")
    List<TripSeat> findByLockedBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * Find all seats with expired locks.
     * Used by the scheduler to auto-release expired locks.
     */
    @Query("SELECT ts FROM TripSeat ts WHERE ts.status = 'Held' AND ts.holdExpiry < :now")
    List<TripSeat> findExpiredLocks(@Param("now") LocalDateTime now);
    
    /**
     * Find all locked seats for a specific trip.
     */
    @Query("SELECT ts FROM TripSeat ts WHERE ts.trip.tripId = :tripId AND ts.status = 'Held'")
    List<TripSeat> findLockedSeatsByTripId(@Param("tripId") Integer tripId);
    
    /**
     * Bulk release expired locks (more efficient for batch operations).
     * Returns the number of updated rows.
     */
    @Modifying
    @Query("UPDATE TripSeat ts SET ts.status = 'Available', ts.lockedBy = null, ts.lockedBySessionId = null, ts.holdExpiry = null " +
           "WHERE ts.status = 'Held' AND ts.holdExpiry < :now")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);
    
    /**
     * Release all locks held by a specific session.
     * Returns the number of updated rows.
     */
    @Modifying
    @Query("UPDATE TripSeat ts SET ts.status = 'Available', ts.lockedBy = null, ts.lockedBySessionId = null, ts.holdExpiry = null " +
           "WHERE ts.lockedBySessionId = :sessionId AND ts.status = 'Held'")
    int releaseAllLocksBySessionId(@Param("sessionId") String sessionId);
    
    /**
     * Find seat by ID and trip ID (without lock).
     */
    @Query("SELECT ts FROM TripSeat ts WHERE ts.seatId = :seatId AND ts.trip.tripId = :tripId")
    Optional<TripSeat> findBySeatIdAndTripId(@Param("seatId") Integer seatId, @Param("tripId") Integer tripId);
}
