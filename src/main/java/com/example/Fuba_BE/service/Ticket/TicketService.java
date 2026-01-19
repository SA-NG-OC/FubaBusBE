package com.example.Fuba_BE.service.Ticket;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.enums.TicketStatus;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInRequestDTO;
import com.example.Fuba_BE.dto.Ticket.TicketCheckInResponseDTO;
import com.example.Fuba_BE.dto.Ticket.TicketExportDTO;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.TicketMapper;
import com.example.Fuba_BE.repository.PassengerRepository;
import com.example.Fuba_BE.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with code: " + ticketCode));

        Passenger passenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId())
                .orElse(null);

        return ticketMapper.toScanResponse(ticket, passenger);
    }

    /**
     * DEPRECATED: No longer used in simplified flow
     * 
     * Old check-in flow used this method to set ticket to CheckedIn status.
     * New flow uses confirmTicket() directly to set status to Used.
     * 
     * Kept for reference/legacy support if needed.
     */
    @Override
    @Transactional
    public TicketCheckInResponseDTO checkInTicket(TicketCheckInRequestDTO request) {
        log.info("Processing legacy check-in for ticket code: {}", request.getTicketCode());

        Ticket ticket = ticketRepository.findByTicketCodeWithLock(request.getTicketCode())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Ticket not found with code: " + request.getTicketCode()));

        Passenger passenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId()).orElse(null);

        String previousStatus = ticket.getTicketStatus();
        // Reuse existing validators to ensure ticket can be checked/confirmed
        validateTicketForCheckIn(ticket);

        Trip trip = ticket.getBooking().getTrip();

        if (request.getTripId() != null) {
            validateCorrectTrip(trip, request.getTripId());
        }

        if (request.getVehicleId() != null) {
            validateCorrectVehicle(trip, request.getVehicleId());
        }

        validateTripDate(trip);
        validateCheckInTime(trip);

        // In simplified flow, treat legacy check-in as immediate confirmation (Used)
        ticket.setTicketStatus(TicketStatus.USED.getDisplayName());
        ticketRepository.save(ticket);

        log.info("Ticket {} legacy-checked successfully. Status changed from {} to Used",
                ticket.getTicketCode(), previousStatus);

        String passengerName = getPassengerName(passenger, ticket);
        String routeName = buildRouteName(trip);
        String licensePlate = trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : null;

        return TicketCheckInResponseDTO.builder()
                .ticketCode(ticket.getTicketCode())
                .previousStatus(previousStatus)
                .newStatus(TicketStatus.USED.getDisplayName())
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

    // --- [NEW] Method lấy dữ liệu thật để xuất PDF ---
    @Override
    @Transactional(readOnly = true)
    public TicketExportDTO getTicketExportData(Integer ticketId) {
        // 1. Lấy thông tin vé từ DB
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        var booking = ticket.getBooking();
        var trip = booking.getTrip();
        var route = trip.getRoute();
        var vehicle = trip.getVehicle();
        var driver = trip.getDriver();
        var seat = ticket.getSeat();

        // 2. Format Tiền (VND)
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String formattedPrice = currencyFormatter.format(ticket.getPrice());

        // 3. Format Ngày Giờ
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 4. Lấy thông tin hành khách
        Passenger passenger = passengerRepository.findByTicket_TicketId(ticketId).orElse(null);
        String pName = (passenger != null) ? passenger.getFullName() : booking.getCustomerName();
        String pPhone = (passenger != null) ? passenger.getPhoneNumber() : booking.getCustomerPhone();
        String pEmail = (passenger != null) ? passenger.getEmail() : booking.getCustomerEmail();

        // 5. Build DTO
        return TicketExportDTO.builder()
                // Header
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getTicketStatus())
                .qrCodeBase64("")

                // Timeline
                .pickupTime(trip.getDepartureTime().format(timeFormatter))
                .pickupLocation(route != null ? route.getOrigin().getLocationName() : "Unknown Origin")
                .dropoffTime(trip.getArrivalTime().format(timeFormatter))
                .dropoffLocation(route != null ? route.getDestination().getLocationName() : "Unknown Destination")

                // --- [SỬA LỖI Ở ĐÂY] ---
                // Thay trip.getDate() bằng trip.getDepartureTime()
                .departureDate(trip.getDepartureTime().format(dateFormatter))
                // -----------------------

                // Trip Info
                .vehicleType(vehicle != null ? vehicle.getVehicleType().getTypeName() : "Standard Bus")
                .licensePlate(vehicle != null ? vehicle.getLicensePlate() : "Đang cập nhật")
                .driverName(driver != null ? driver.getUser().getFullName() : "Đang cập nhật")

                // Passenger Info
                .passengerName(pName)
                .passengerPhone(pPhone)
                .passengerEmail(pEmail != null ? pEmail : "")

                // Seat & Price
                .seatNumber(seat.getSeatNumber())
                .seatFloor(seat.getFloorNumber() == 1 ? "Tầng dưới" : "Tầng trên")
                .totalPrice(formattedPrice)
                .build();
    }

    /**
     * SIMPLIFIED TICKET CONFIRMATION FLOW
     * 
     * Purpose: Staff confirms ticket usage after scanning QR code
     * 
     * Flow:
     * 1. Staff scans QR code → mobile app calls GET /tickets/{ticketCode}
     * 2. App shows ticket details (passenger name, seat, route, status)
     * 3. Staff clicks "Xác nhận" button → calls POST /tickets/{ticketCode}/confirm
     * 4. This method changes status: Confirmed → Used
     * 5. Passenger can board the bus
     * 
     * Status meaning:
     * - Confirmed: Ticket is paid and valid (ready to use)
     * - Used: Ticket has been validated by staff, passenger boarded
     * 
     * Note: Old flow had "CheckedIn" status but it's no longer used.
     * We simplified: Confirmed → Used (one step instead of two)
     */
    @Override
    @Transactional
    public boolean confirmTicket(String ticketCode) {
        log.info("Confirming ticket: {}", ticketCode);

        // 1. Find ticket with pessimistic lock (prevent concurrent confirmations)
        Ticket ticket = ticketRepository.findByTicketCodeWithLock(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé: " + ticketCode));

        // 2. Get current status
        String currentStatus = ticket.getTicketStatus();
        log.info("Current ticket status: {}", currentStatus);

        // 3. Validate and update status
        if ("Confirmed".equals(currentStatus)) {
            // Normal flow: Confirmed → Used
            ticket.setTicketStatus(TicketStatus.USED.getDisplayName());
            ticketRepository.save(ticket);
            log.info("Ticket {} confirmed successfully. Status changed to Used", ticketCode);
            return true;
        } else if ("CheckedIn".equals(currentStatus)) {
            // Legacy support: Some old tickets might have CheckedIn status
            // Also allow CheckedIn → Used transition
            ticket.setTicketStatus(TicketStatus.USED.getDisplayName());
            ticketRepository.save(ticket);
            log.warn("Ticket {} had legacy CheckedIn status, now changed to Used", ticketCode);
            return true;
        } else if ("Used".equals(currentStatus)) {
            // Already used - reject
            throw new BadRequestException("Vé này đã được xác nhận sử dụng rồi.");
        } else if ("Unconfirmed".equals(currentStatus)) {
            // Not paid yet
            throw new BadRequestException("Vé chưa được thanh toán/xác nhận.");
        } else if ("Cancelled".equals(currentStatus)) {
            // Cancelled
            throw new BadRequestException("Vé đã bị hủy, không thể sử dụng.");
        } else {
            // Other invalid statuses
            throw new BadRequestException("Vé không hợp lệ. Trạng thái: " + currentStatus);
        }
    }

    // --- Private Validators & Helpers ---

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
                break;
            default:
                throw new BadRequestException("Invalid ticket status: " + status);
        }
    }

    private void validateCorrectTrip(Trip ticketTrip, Integer requestedTripId) {
        if (!ticketTrip.getTripId().equals(requestedTripId)) {
            throw new BadRequestException(
                    String.format("Wrong trip! This ticket is for trip #%d, but you are checking in for trip #%d",
                            ticketTrip.getTripId(), requestedTripId));
        }
    }

    private void validateCorrectVehicle(Trip trip, Integer requestedVehicleId) {
        if (trip.getVehicle() == null) {
            throw new BadRequestException("Trip has no vehicle assigned");
        }
        if (!trip.getVehicle().getVehicleId().equals(requestedVehicleId)) {
            throw new BadRequestException(
                    String.format("Wrong vehicle! This ticket is for vehicle %s, not the current vehicle",
                            trip.getVehicle().getLicensePlate()));
        }
    }

    private void validateTripDate(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = trip.getDepartureTime();
        if (!now.toLocalDate().equals(departureTime.toLocalDate())) {
            throw new BadRequestException(
                    String.format("Wrong date! This ticket is for %s, but today is %s",
                            departureTime.toLocalDate(), now.toLocalDate()));
        }
    }

    private void validateCheckInTime(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departureTime = trip.getDepartureTime();
        LocalDateTime arrivalTime = trip.getArrivalTime();

        LocalDateTime earliestCheckIn = departureTime.minusHours(2);
        if (now.isBefore(earliestCheckIn)) {
            throw new BadRequestException(
                    "Check-in is not available yet. Check-in opens 2 hours before departure at " + earliestCheckIn);
        }

        if (now.isAfter(arrivalTime)) {
            throw new BadRequestException("Cannot check-in. This trip has already ended");
        }

        if (now.isAfter(departureTime)) {
            log.warn("Late check-in for trip departing at {}. Current time: {}", departureTime, now);
        }
    }

    private String getPassengerName(Passenger passenger, Ticket ticket) {
        if (passenger != null && passenger.getFullName() != null) {
            return passenger.getFullName();
        }
        return ticket.getBooking().getCustomerName();
    }

    private String buildRouteName(Trip trip) {
        if (trip.getRoute() == null)
            return "Unknown Route";
        return trip.getRoute().getOrigin().getLocationName() + " → " +
                trip.getRoute().getDestination().getLocationName();
    }
}