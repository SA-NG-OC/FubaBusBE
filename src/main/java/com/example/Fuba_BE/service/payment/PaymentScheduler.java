package com.example.Fuba_BE.service.payment;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.dto.payment.MomoPaymentResponse;
import com.example.Fuba_BE.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled jobs for payment operations
 * Automatically checks pending payments and syncs with MoMo
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final BookingRepository bookingRepository;
    private final MomoPaymentService momoPaymentService;

    /**
     * Scheduled task to check pending payments that are stuck.
     * Runs every 5 minutes to query MoMo for actual payment status.
     * 
     * This handles cases where:
     * - IPN callback was missed or failed
     * - Network issues prevented IPN delivery
     * - User paid but booking is still Pending
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    @Transactional
    public void checkPendingPayments() {
        try {
            log.debug("🔍 Running scheduled task to check pending payments");
            
            // Find bookings that are Pending for more than 2 minutes (IPN should arrive within 1-2 mins)
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
            List<Booking> pendingBookings = bookingRepository
                .findByBookingStatusAndUpdatedAtBefore("Pending", threshold);
            
            if (pendingBookings.isEmpty()) {
                log.debug("No pending payments to check");
                return;
            }
            
            log.info("📊 Found {} pending bookings older than 2 minutes. Querying MoMo...", 
                pendingBookings.size());
            
            int checked = 0;
            int synced = 0;
            int errors = 0;
            
            for (Booking booking : pendingBookings) {
                 // 🚨 CRITICAL: NEVER touch PAID bookings
    if ("Paid".equals(booking.getBookingStatus())) {
        log.debug("⏭️ Skip booking {} – already Paid", booking.getBookingCode());
        continue;
    }
                try {
                    // Skip if booking is about to expire (will be handled by expireHeldBookings)
                    if (booking.getHoldExpiry() != null && 
                        LocalDateTime.now().isAfter(booking.getHoldExpiry())) {
                        log.debug("⏭️ Skipping booking {} - will expire soon", booking.getBookingCode());
                        continue;
                    }
                    
                    checked++;
                    log.debug("🔎 Checking payment status for booking: {}", booking.getBookingCode());
                    
                    // Query MoMo for actual payment status
                    // Note: We need requestId which should be stored in booking or payment record
                    // For now, we'll log that we need to store requestId
                    log.warn("⚠️ Cannot query MoMo status for booking {} - requestId not stored in database", 
                        booking.getBookingCode());
                    
                    // TODO: Store requestId in Booking or create Payment entity to track this
                    // MomoPaymentResponse status = momoPaymentService.queryPaymentStatus(
                    //     booking.getBookingCode(), 
                    //     booking.getRequestId() // Need to add this field
                    // );
                    
                    // if (status.getResultCode() == 0) {
                    //     log.info("✅ MoMo confirms payment successful for {}, but IPN was missed!", 
                    //         booking.getBookingCode());
                    //     // Would need to create IPN request from query response and call handleIpnCallback
                    //     synced++;
                    // }
                    
                } catch (Exception e) {
                    log.error("❌ Error checking payment for booking {}: {}", 
                        booking.getBookingCode(), e.getMessage());
                    errors++;
                }
            }
            
            log.info("📈 Payment check completed: {} checked, {} synced, {} errors", 
                checked, synced, errors);
            
        } catch (Exception e) {
            log.error("❌ Error in scheduled payment check task: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task to clean up old PaymentFailed bookings
     * Runs daily to delete or archive failed payments older than 7 days
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupFailedPayments() {
        try {
            log.info("🧹 Running cleanup of old PaymentFailed bookings");
            
            LocalDateTime threshold = LocalDateTime.now().minusDays(7);
            List<Booking> failedBookings = bookingRepository
                .findByBookingStatusAndUpdatedAtBefore("PaymentFailed", threshold);
            
            if (failedBookings.isEmpty()) {
                log.debug("No failed payments to clean up");
                return;
            }
            
            log.info("Found {} failed bookings older than 7 days", failedBookings.size());
            
            // Option 1: Delete them
            // bookingRepository.deleteAll(failedBookings);
            
            // Option 2: Archive them (update status to Archived)
            for (Booking booking : failedBookings) {
                booking.setBookingStatus("Archived");
            }
            bookingRepository.saveAll(failedBookings);
            
            log.info("✅ Archived {} failed payment bookings", failedBookings.size());
            
        } catch (Exception e) {
            log.error("❌ Error in cleanup failed payments task: {}", e.getMessage(), e);
        }
    }
}
