package com.example.Fuba_BE.config;

import com.example.Fuba_BE.service.SeatLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduler configuration for automatic seat lock expiration.
 * Periodically checks for and releases expired seat locks.
 */
@Configuration
@EnableScheduling
public class SeatLockSchedulerConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SeatLockSchedulerConfig.class);
    
    private final SeatLockService seatLockService;
    
    public SeatLockSchedulerConfig(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }
    
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
     * Alternative: More frequent check (every 10 seconds) for higher responsiveness.
     * Uncomment this method and comment out the above if needed.
     */
    // @Scheduled(fixedRate = 10000)
    // public void releaseExpiredLocksFrequent() {
    //     releaseExpiredLocks();
    // }
}
