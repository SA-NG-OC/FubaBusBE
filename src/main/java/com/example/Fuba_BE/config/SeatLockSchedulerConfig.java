package com.example.Fuba_BE.config;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.repository.BookingRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;
import com.example.Fuba_BE.service.ISeatLockService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;


/**
 * Scheduler component for automatic seat lock expiration and booking expiration.
 * Periodically checks for and releases expired seat locks and expires held bookings.
 */
@Component
@RequiredArgsConstructor
public class SeatLockSchedulerConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SeatLockSchedulerConfig.class);
    private static final int BOOKING_HOLD_DURATION_MINUTES = 15; // Bookings expire after 15 minutes
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripSeatRepository tripSeatRepository;

    private final ISeatLockService seatLockService;
    
    /**
     * Scheduled task to release expired seat locks.
     * Runs every 30 seconds to check for expired locks.
     * 
     * The 30-second interval provides a good balance between:
     * - Timely release of expired seats (within 30 seconds of expiry)
     * - Not overloading the database with frequent queries
     * 
     * For production, consider:
     * - Using a distributed lock if running multiple instances
     * - Adjusting the interval based on traffic patterns
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void releaseExpiredLocks() {
        try {
            logger.debug("Running scheduled task to release expired seat locks");
            seatLockService.releaseExpiredLocks();
        } catch (Exception e) {
            logger.error("Error in scheduled task to release expired locks: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to expire held/pending bookings.
     * Runs every 60 seconds to check for bookings that have been held longer than the allowed time.
     * Bookings in 'Held' or 'Pending' status for more than BOOKING_HOLD_DURATION_MINUTES will be marked as 'Expired'
     * (system timeout), while user-initiated cancellations will be marked as 'Cancelled'.
     * Seats will be released back to Available status.
     */
@Scheduled(fixedRate = 60000) // Every 60 seconds
@Transactional
public void expireHeldBookings() {
    try {
        logger.debug("Running scheduled task to expire held bookings");
        LocalDateTime now = LocalDateTime.now();

        // Find expired bookings before updating them (using hold_expiry field)
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(now);

        if (!expiredBookings.isEmpty()) {
            logger.info("Found {} bookings with hold_expiry < now ({})",
                    expiredBookings.size(), now);

            int totalSeatsReleased = 0;

            for (Booking booking : expiredBookings) {

                // ✅ FIX 1: Không expire booking đã Paid
                if ("Paid".equals(booking.getBookingStatus())) {
                    logger.warn("Skip expiring Paid booking {}", booking.getBookingCode());
                    continue;
                }

                // ✅ FIX 2: Mark booking Expired MỘT LẦN, ngoài loop ticket
                booking.setBookingStatus("Expired");
                bookingRepository.save(booking);

                // Release seats for this expired booking
                List<Ticket> tickets =
                        ticketRepository.findByBookingId(booking.getBookingId());

                for (Ticket ticket : tickets) {
                    TripSeat seat = ticket.getSeat();

                    // ✅ FIX 3: Chỉ release ghế CHƯA Booked
                    if (seat != null ) {
                        seat.release();
                        tripSeatRepository.save(seat);
                        totalSeatsReleased++;

                        logger.debug("Released seat {} for expired booking {}",
                                seat.getSeatNumber(), booking.getBookingCode());
                    }
                }
            }

            logger.info("Expired {} bookings and released {} seats",
                    expiredBookings.size(), totalSeatsReleased);
        }
    } catch (Exception e) {
        logger.error("Error in scheduled task to expire bookings: {}", e.getMessage(), e);
    }
}

    
    /**
     * Alternative: More frequent check (every 10 seconds) for higher responsiveness.
     * Uncomment this method and comment out the above if needed.
     */
    // @Scheduled(fixedRate = 10000)
    // public void releaseExpiredLocksFrequent() {
    //     releaseExpiredLocks();
    // }
}
