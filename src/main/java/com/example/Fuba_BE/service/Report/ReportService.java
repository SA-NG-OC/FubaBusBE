package com.example.Fuba_BE.service.Report;

import com.example.Fuba_BE.domain.enums.TripStatus;
import com.example.Fuba_BE.dto.Report.TicketSalesReportDTO;
import com.example.Fuba_BE.dto.Report.TripOperationReportDTO;
import com.example.Fuba_BE.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ReportService implements IReportService {

    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public TripOperationReportDTO getTripOperationReport(LocalDate fromDate, LocalDate toDate, Integer routeId) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        long total = tripRepository.countTrips(start, end, routeId);
        long completed = tripRepository.countTripsByStatus(start, end, TripStatus.COMPLETED.getDisplayName(), routeId);
        long cancelled = tripRepository.countTripsByStatus(start, end, TripStatus.CANCELLED.getDisplayName(), routeId);
        long delayed = tripRepository.countTripsByStatus(start, end, TripStatus.DELAYED.getDisplayName(), routeId);
        long running = tripRepository.countTripsByStatus(start, end, TripStatus.RUNNING.getDisplayName(), routeId);
        long waiting = tripRepository.countTripsByStatus(start, end, TripStatus.WAITING.getDisplayName(), routeId);
        BigDecimal revenue = tripRepository.sumTripBasePrice(start, end, routeId);

        return TripOperationReportDTO.builder()
                .totalTrips(total)
                .totalCompleted(completed)
                .totalCancelled(cancelled)
                .totalDelayed(delayed)
                .totalRunning(running)
                .totalWaiting(waiting)
                .estimatedRevenue(revenue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketSalesReportDTO getTicketSalesReport(LocalDate fromDate, LocalDate toDate, Integer routeId) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        long totalSeats = tripRepository.countTotalSeats(start, end, routeId);
        long soldTickets = tripRepository.countSoldTickets(start, end, routeId);
        BigDecimal revenue = tripRepository.sumTicketRevenue(start, end, routeId);

        double occupancyRate = 0.0;
        if (totalSeats > 0) {
            occupancyRate = (double) soldTickets / totalSeats * 100;
        }

        return TicketSalesReportDTO.builder()
                .totalSeats(totalSeats)
                .ticketsSold(soldTickets)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .totalRevenue(revenue)
                .build();
    }
}