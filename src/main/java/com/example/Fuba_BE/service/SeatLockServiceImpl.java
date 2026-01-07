package com.example.Fuba_BE.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.dto.seat.SeatStatusMessage;
import com.example.Fuba_BE.repository.TripSeatRepository;

/**
 * Implementation of ISeatLockService for real-time seat locking.
 * Uses pessimistic locking (SELECT FOR UPDATE) for concurrency safety.
 */
@Service
public class SeatLockServiceImpl implements ISeatLockService {
    
    private static final Logger logger = LoggerFactory.getLogger(SeatLockServiceImpl.class);
    
    private final TripSeatRepository tripSeatRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Track session to seats mapping for efficient disconnect handling
    private final Map<String, List<Integer>> sessionSeatMap = new ConcurrentHashMap<>();
    
    public SeatLockServiceImpl(TripSeatRepository tripSeatRepository, 
                               SimpMessagingTemplate messagingTemplate) {
        this.tripSeatRepository = tripSeatRepository;
        this.messagingTemplate = messagingTemplate;
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SeatStatusMessage lockSeat(Integer seatId, Integer tripId, String userId, String sessionId) {
        logger.info("Attempting to lock seat {} for user {} (session: {})", seatId, userId, sessionId);
        
        try {
            // Use pessimistic lock to prevent race conditions
            Optional<TripSeat> seatOpt = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, tripId);
            
            if (seatOpt.isEmpty()) {
                logger.warn("Seat {} not found for trip {}", seatId, tripId);
                return SeatStatusMessage.lockFailed(seatId, tripId, "Seat not found");
            }
            
            TripSeat seat = seatOpt.get();
            
            // Check if seat is available or if the lock has expired
            if (seat.isLocked()) {
                if (seat.isLockExpired()) {
                    // Lock has expired, release it first
                    logger.info("Releasing expired lock on seat {}", seatId);
                    seat.release();
                } else if (seat.getLockedBy() != null && seat.getLockedBy().equals(userId)) {
                    // Same user re-locking (e.g., page refresh) - extend the lock
                    logger.info("Extending lock for user {} on seat {}", userId, seatId);
                    seat.lock(userId, sessionId, LOCK_DURATION_MINUTES);
                    TripSeat savedSeat = tripSeatRepository.save(seat);
                    
                    // Update session mapping
                    updateSessionSeatMapping(sessionId, seatId);
                    
                    return SeatStatusMessage.locked(
                            savedSeat.getSeatId(),
                            savedSeat.getSeatNumber(),
                            tripId,
                            userId,
                            savedSeat.getHoldExpiry(),
                            savedSeat.getFloorNumber()
                    );
                } else {
                    // Someone else has it locked
                    logger.info("Seat {} is already locked by another user", seatId);
                    return SeatStatusMessage.lockFailed(seatId, tripId, 
                            "Seat is currently locked by another user");
                }
            }
            
            if (seat.isBooked()) {
                logger.info("Seat {} is already booked", seatId);
                return SeatStatusMessage.lockFailed(seatId, tripId, "Seat is already booked");
            }
            
            // Lock the seat
            seat.lock(userId, sessionId, LOCK_DURATION_MINUTES);
            TripSeat savedSeat = tripSeatRepository.save(seat);
            
            // Update session mapping
            updateSessionSeatMapping(sessionId, seatId);
            
            logger.info("Successfully locked seat {} for user {} until {}", 
                    seatId, userId, savedSeat.getHoldExpiry());
            
            return SeatStatusMessage.locked(
                    savedSeat.getSeatId(),
                    savedSeat.getSeatNumber(),
                    tripId,
                    userId,
                    savedSeat.getHoldExpiry(),
                    savedSeat.getFloorNumber()
            );
            
        } catch (Exception e) {
            logger.error("Error locking seat {}: {}", seatId, e.getMessage(), e);
            return SeatStatusMessage.lockFailed(seatId, tripId, "Internal error: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SeatStatusMessage unlockSeat(Integer seatId, Integer tripId, String userId, String sessionId) {
        logger.info("Attempting to unlock seat {} by user {} (session: {})", seatId, userId, sessionId);
        
        try {
            Optional<TripSeat> seatOpt = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, tripId);
            
            if (seatOpt.isEmpty()) {
                logger.warn("Seat {} not found for trip {}", seatId, tripId);
                return SeatStatusMessage.unlockFailed(seatId, tripId, "Seat not found");
            }
            
            TripSeat seat = seatOpt.get();
            
            // Check if seat is locked
            if (!seat.isLocked()) {
                logger.info("Seat {} is not locked", seatId);
                return SeatStatusMessage.unlockFailed(seatId, tripId, "Seat is not locked");
            }
            
            // Verify ownership - allow unlock if user owns the lock OR session owns it
            boolean isOwner = (seat.getLockedBy() != null && seat.getLockedBy().equals(userId)) ||
                             (seat.getLockedBySessionId() != null && seat.getLockedBySessionId().equals(sessionId));
            
            if (!isOwner) {
                logger.warn("User {} (session: {}) attempted to unlock seat {} owned by {} (session: {})", 
                        userId, sessionId, seatId, seat.getLockedBy(), seat.getLockedBySessionId());
                return SeatStatusMessage.unlockFailed(seatId, tripId, "Not authorized to unlock this seat");
            }
            
            // Release the lock
            seat.release();
            TripSeat savedSeat = tripSeatRepository.save(seat);
            
            // Remove from session mapping
            removeFromSessionSeatMapping(sessionId, seatId);
            
            logger.info("Successfully unlocked seat {}", seatId);
            
            return SeatStatusMessage.unlocked(
                    savedSeat.getSeatId(),
                    savedSeat.getSeatNumber(),
                    tripId,
                    savedSeat.getFloorNumber()
            );
            
        } catch (Exception e) {
            logger.error("Error unlocking seat {}: {}", seatId, e.getMessage(), e);
            return SeatStatusMessage.unlockFailed(seatId, tripId, "Internal error: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SeatStatusMessage confirmBooking(Integer seatId, Integer tripId, String userId) {
        logger.info("Confirming booking for seat {} by user {}", seatId, userId);
        
        try {
            Optional<TripSeat> seatOpt = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, tripId);
            
            if (seatOpt.isEmpty()) {
                logger.warn("Seat {} not found for trip {}", seatId, tripId);
                return SeatStatusMessage.lockFailed(seatId, tripId, "Seat not found");
            }
            
            TripSeat seat = seatOpt.get();
            
            // Verify the seat is locked by this user
            if (!seat.isLocked()) {
                logger.warn("Cannot book seat {} - not locked", seatId);
                return SeatStatusMessage.lockFailed(seatId, tripId, "Seat must be locked before booking");
            }
            
            if (seat.getLockedBy() == null || !seat.getLockedBy().equals(userId)) {
                logger.warn("User {} cannot book seat {} - locked by {}", userId, seatId, seat.getLockedBy());
                return SeatStatusMessage.lockFailed(seatId, tripId, "You don't have a lock on this seat");
            }
            
            // Check if lock has expired
            if (seat.isLockExpired()) {
                logger.warn("Lock on seat {} has expired", seatId);
                seat.release();
                tripSeatRepository.save(seat);
                return SeatStatusMessage.lockFailed(seatId, tripId, "Lock has expired, please try again");
            }
            
            // Book the seat
            String sessionId = seat.getLockedBySessionId();
            seat.book();
            TripSeat savedSeat = tripSeatRepository.save(seat);
            
            // Remove from session mapping
            if (sessionId != null) {
                removeFromSessionSeatMapping(sessionId, seatId);
            }
            
            logger.info("Successfully booked seat {} for user {}", seatId, userId);
            
            return SeatStatusMessage.booked(
                    savedSeat.getSeatId(),
                    savedSeat.getSeatNumber(),
                    tripId,
                    userId,
                    savedSeat.getFloorNumber()
            );
            
        } catch (Exception e) {
            logger.error("Error confirming booking for seat {}: {}", seatId, e.getMessage(), e);
            return SeatStatusMessage.lockFailed(seatId, tripId, "Internal error: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public List<SeatStatusMessage> releaseAllBySession(String sessionId) {
        logger.info("Releasing all seats for session {}", sessionId);
        
        List<SeatStatusMessage> releasedSeats = new ArrayList<>();
        
        try {
            List<TripSeat> lockedSeats = tripSeatRepository.findByLockedBySessionId(sessionId);
            
            for (TripSeat seat : lockedSeats) {
                Integer tripId = seat.getTrip().getTripId();
                seat.release();
                TripSeat savedSeat = tripSeatRepository.save(seat);
                
                SeatStatusMessage message = SeatStatusMessage.unlocked(
                        savedSeat.getSeatId(),
                        savedSeat.getSeatNumber(),
                        tripId,
                        savedSeat.getFloorNumber()
                );
                releasedSeats.add(message);
                
                // Broadcast to the trip's topic
                broadcastToTripTopic(tripId, message);
            }
            
            // Clear session mapping
            sessionSeatMap.remove(sessionId);
            
            logger.info("Released {} seats for session {}", releasedSeats.size(), sessionId);
            
        } catch (Exception e) {
            logger.error("Error releasing seats for session {}: {}", sessionId, e.getMessage(), e);
        }
        
        return releasedSeats;
    }
    
    @Override
    @Transactional
    public List<SeatStatusMessage> releaseExpiredLocks() {
        logger.debug("Checking for expired seat locks");
        
        List<SeatStatusMessage> releasedSeats = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        try {
            List<TripSeat> expiredSeats = tripSeatRepository.findExpiredLocks(now);
            
            for (TripSeat seat : expiredSeats) {
                Integer tripId = seat.getTrip().getTripId();
                String sessionId = seat.getLockedBySessionId();
                
                seat.release();
                TripSeat savedSeat = tripSeatRepository.save(seat);
                
                // Remove from session mapping
                if (sessionId != null) {
                    removeFromSessionSeatMapping(sessionId, seat.getSeatId());
                }
                
                SeatStatusMessage message = SeatStatusMessage.expired(
                        savedSeat.getSeatId(),
                        savedSeat.getSeatNumber(),
                        tripId,
                        savedSeat.getFloorNumber()
                );
                releasedSeats.add(message);
                
                // Broadcast to the trip's topic
                broadcastToTripTopic(tripId, message);
                
                logger.info("Released expired lock on seat {} (was held by {})", 
                        seat.getSeatId(), seat.getLockedBy());
            }
            
            if (!releasedSeats.isEmpty()) {
                logger.info("Released {} expired seat locks", releasedSeats.size());
            }
            
        } catch (Exception e) {
            logger.error("Error releasing expired locks: {}", e.getMessage(), e);
        }
        
        return releasedSeats;
    }
    
    /**
     * Broadcast a message to all subscribers of a trip's seat topic.
     */
    public void broadcastToTripTopic(Integer tripId, SeatStatusMessage message) {
        String destination = getTripTopic(tripId);
        messagingTemplate.convertAndSend(destination, message);
        logger.debug("Broadcasted to {}: {}", destination, message.getType());
    }
    
    /**
     * Update the session-to-seats mapping.
     */
    private void updateSessionSeatMapping(String sessionId, Integer seatId) {
        sessionSeatMap.computeIfAbsent(sessionId, k -> new ArrayList<>())
                     .add(seatId);
    }
    
    /**
     * Remove a seat from the session mapping.
     */
    private void removeFromSessionSeatMapping(String sessionId, Integer seatId) {
        List<Integer> seats = sessionSeatMap.get(sessionId);
        if (seats != null) {
            seats.remove(seatId);
            if (seats.isEmpty()) {
                sessionSeatMap.remove(sessionId);
            }
        }
    }
    
    /**
     * Get seats locked by a session.
     */
    public List<Integer> getSeatsLockedBySession(String sessionId) {
        return sessionSeatMap.getOrDefault(sessionId, new ArrayList<>());
    }
}
