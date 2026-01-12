package com.example.Fuba_BE.service.Ticket;

import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.TicketMapper;
import com.example.Fuba_BE.repository.PassengerRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService implements ITicketService {

    private final TicketRepository ticketRepository;
    private final PassengerRepository passengerRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public TicketScanResponseDTO getTicketDetailsByCode(String ticketCode) {
        // 1. Fetch Ticket with deep fetching (using EntityGraph or JoinFetch in Repo is recommended to avoid N+1)
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        // 2. Fetch Passenger associated with this ticket (Optional)
        Passenger passenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId())
                .orElse(null);

        // 3. Use Mapper to build response
        return ticketMapper.toScanResponse(ticket, passenger);
    }

    @Override
    @Transactional
    public TicketCheckInResponseDTO checkInTicket(TicketCheckInRequestDTO request) {
        log.info("Processing check-in for ticket code: {}", request.getTicketCode());

        // 1. Find ticket with lock to prevent concurrent check-in
        Ticket ticket = ticketRepository.findByTicketCodeWithLock(request.getTicketCode())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + request.getTicketCode()));

        // 2. Get passenger info
        Passenger passenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId())
                .orElse(null);

        // 3. Validate ticket status
        String previousStatus = ticket.getTicketStatus();
        validateTicketForCheckIn(ticket);

        // 4. Get trip info for validation
        Trip trip = ticket.getBooking().getTrip();

        // 5. Validate correct trip (if tripId provided)
        if (request.getTripId() != null) {
            validateCorrectTrip(trip, request.getTripId());
        }

        // 6. Validate correct vehicle (if vehicleId provided)
        if (request.getVehicleId() != null) {
            validateCorrectVehicle(trip, request.getVehicleId());
        }

        // 7. Validate trip date - must be today or departure day
        validateTripDate(trip);

        // 8. Validate check-in time (check-in should be within reasonable time before departure)
        validateCheckInTime(trip);

        // 9. Update ticket status to "Used"
        ticket.setTicketStatus("Used");
        ticketRepository.save(ticket);

        log.info("Ticket {} checked in successfully. Status changed from {} to Used", 
                ticket.getTicketCode(), previousStatus);

        // 10. Build response
        String passengerName = getPassengerName(passenger, ticket);
        String routeName = buildRouteName(trip);
        String licensePlate = trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : null;

        return TicketCheckInResponseDTO.builder()
                .ticketCode(ticket.getTicketCode())
                .previousStatus(previousStatus)
                .newStatus("Used")
                .checkInTime(LocalDateTime.now())
                .checkInMethod(request.getCheckInMethod())
                .passengerName(passengerName)
                .seatNumber(ticket.getSeat().getSeatNumber())
                .routeName(routeName)
                .departureTime(trip.getDepartureTime())
                .tripId(trip.getTripId())
                .licensePlate(licensePlate)
                .message("Check-in successful! Welcome aboard.")
                .build();
    }

    /**
     * Validate if ticket can be checked in
     */
    private void validateTicketForCheckIn(Ticket ticket) {
        String status = ticket.getTicketStatus();

        switch (status) {
            case "Used":
                throw new BadRequestException("Ticket has already been used for check-in");
            case "Cancelled":
                throw new BadRequestException("Cannot check-in a cancelled ticket");
            case "Refunded":
                throw new BadRequestException("Cannot check-in a refunded ticket");
            case "Unconfirmed":
                throw new BadRequestException("Ticket payment has not been confirmed. Please complete payment first");
            case "Confirmed":
                // Valid for check-in
                break;
            default:
                throw new BadRequestException("Invalid ticket status: " + status);
        }
    }

    /**
     * Validate ticket belongs to the correct trip
     */
    private void validateCorrectTrip(Trip ticketTrip, Integer requestedTripId) {
        if (!ticketTrip.getTripId().equals(requestedTripId)) {
            throw new BadRequestException(
                String.format("Wrong trip! This ticket is for trip #%d, but you are checking in for trip #%d",
                    ticketTrip.getTripId(), requestedTripId)
            );
        }
    }

    /**
     * Validate ticket belongs to the correct vehicle
     */
    private void validateCorrectVehicle(Trip trip, Integer requestedVehicleId) {
        if (trip.getVehicle() == null) {
            throw new BadRequestException("Trip has no vehicle assigned");
        }
        
        if (!trip.getVehicle().getVehicleId().equals(requestedVehicleId)) {
            throw new BadRequestException(
                String.format("Wrong vehicle! This ticket is for vehicle %s, not the current vehicle",
                    trip.getVehicle().getLicensePlate())
            );
        }
    }

    /**
     * Validate trip date - check-in only allowed on departure day
     */
    private void validateTripDate(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = trip.getDepartureTime();
        
        // Check if today is the departure date
        if (!now.toLocalDate().equals(departureTime.toLocalDate())) {
            throw new BadRequestException(
                String.format("Wrong date! This ticket is for %s, but today is %s",
                    departureTime.toLocalDate(), now.toLocalDate())
            );
        }
    }

    /**
     * Validate check-in time relative to trip departure
     */
    private void validateCheckInTime(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = trip.getDepartureTime();
        LocalDateTime arrivalTime = trip.getArrivalTime();

        // Cannot check-in more than 2 hours before departure
        LocalDateTime earliestCheckIn = departureTime.minusHours(2);
        if (now.isBefore(earliestCheckIn)) {
            throw new BadRequestException("Check-in is not available yet. Check-in opens 2 hours before departure at " + earliestCheckIn);
        }

        // Cannot check-in after trip has ended
        if (now.isAfter(arrivalTime)) {
            throw new BadRequestException("Cannot check-in. This trip has already ended");
        }

        // Warning if checking in after departure (but allow it - late boarding)
        if (now.isAfter(departureTime)) {
            log.warn("Late check-in for trip departing at {}. Current time: {}", departureTime, now);
        }
    }

    /**
     * Get passenger name from Passenger entity or fallback to Booking info
     */
    private String getPassengerName(Passenger passenger, Ticket ticket) {
        if (passenger != null && passenger.getFullName() != null) {
            return passenger.getFullName();
        }
        return ticket.getBooking().getCustomerName();
    }

    /**
     * Build route name string
     */
    private String buildRouteName(Trip trip) {
        if (trip.getRoute() == null) return "Unknown Route";
        return trip.getRoute().getOrigin().getLocationName() + " → " + 
               trip.getRoute().getDestination().getLocationName();
    }
}