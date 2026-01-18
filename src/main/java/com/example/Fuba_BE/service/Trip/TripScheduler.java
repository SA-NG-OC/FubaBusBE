package com.example.Fuba_BE.service.Trip;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.repository.TripRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled jobs for trip operations.
 * Automatically cancels expired trips that are still in "Waiting" status.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripScheduler {

    private final TripRepository tripRepository;

    /**
     * Scheduled task to automatically cancel expired trips.
     * Runs every 5 minutes (after initial 30s delay) to find trips with
     * status="Waiting"
     * and departure time has already passed.
     * This prevents old trips from blocking new trip creation.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 30000) // Every 5 minutes, start after 30s
    @Transactional
    public void cancelExpiredWaitingTrips() {
        try {
            log.info("🔍 Running scheduled task to cancel expired waiting trips");

            LocalDateTime now = LocalDateTime.now();
            List<Trip> expiredTrips = tripRepository.findExpiredWaitingTrips(now);

            if (expiredTrips.isEmpty()) {
                log.info("✅ No expired waiting trips found");
                return;
            }

            log.info("📋 Found {} expired waiting trips to cancel", expiredTrips.size());

            int cancelledCount = 0;
            for (Trip trip : expiredTrips) {
                try {
                    log.info("🚫 Cancelling expired trip {} (Vehicle: {}, Departure: {})",
                            trip.getTripId(),
                            trip.getVehicle().getLicensePlate(),
                            trip.getDepartureTime());

                    trip.setStatus("Cancelled");
                    tripRepository.save(trip);
                    cancelledCount++;

                } catch (Exception e) {
                    log.error("Failed to cancel trip {}: {}", trip.getTripId(), e.getMessage());
                }
            }

            log.info("Successfully cancelled {}/{} expired waiting trips",
                    cancelledCount, expiredTrips.size());

        } catch (Exception e) {
            log.error("Error in scheduled task cancelExpiredWaitingTrips: {}", e.getMessage(), e);
        }
    }
}
