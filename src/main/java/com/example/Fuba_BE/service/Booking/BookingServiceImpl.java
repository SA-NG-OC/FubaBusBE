package com.example.Fuba_BE.service.Booking;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.BookingMapper;
import com.example.Fuba_BE.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of BookingService.
 * Handles ticket booking with seat lock validation and real-time broadcast.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final TripSeatRepository tripSeatRepository;
    private final TripRepository tripRepository;
    private final PassengerRepository passengerRepository;
    private final UserRepository userRepository;
    private final RouteStopRepository routeStopRepository;
    private final BookingMapper bookingMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(readOnly = true)
    public BookingPreviewResponse previewBooking(Integer tripId, List<Integer> seatIds, String userId) {
        log.info("Previewing booking for trip {} with seats {} by user {}", tripId, seatIds, userId);

        // Validate trip exists
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến đi với ID: " + tripId));

        List<BookingPreviewResponse.SeatInfo> seatInfos = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean allValid = true;
        LocalDateTime earliestExpiry = null;

        for (Integer seatId : seatIds) {
            Optional<TripSeat> seatOpt = tripSeatRepository.findBySeatIdAndTripId(seatId, tripId);
            
            if (seatOpt.isEmpty()) {
                seatInfos.add(BookingPreviewResponse.SeatInfo.builder()
                        .seatId(seatId)
                        .validForBooking(false)
                        .validationMessage("Ghế không tồn tại trong chuyến đi này")
                        .build());
                allValid = false;
                continue;
            }

            TripSeat seat = seatOpt.get();
            boolean isValid = validateSeatForBooking(seat, userId);
            String validationMessage = getValidationMessage(seat, userId);

            BookingPreviewResponse.SeatInfo seatInfo = BookingPreviewResponse.SeatInfo.builder()
                    .seatId(seat.getSeatId())
                    .seatNumber(seat.getSeatNumber())
                    .floorNumber(seat.getFloorNumber())
                    .seatType(seat.getSeatType())
                    .price(trip.getBasePrice())
                    .status(seat.getStatus())
                    .lockedBy(seat.getLockedBy())
                    .lockExpiry(seat.getHoldExpiry())
                    .validForBooking(isValid)
                    .validationMessage(validationMessage)
                    .build();

            seatInfos.add(seatInfo);

            if (isValid) {
                totalAmount = totalAmount.add(trip.getBasePrice());
                if (seat.getHoldExpiry() != null) {
                    if (earliestExpiry == null || seat.getHoldExpiry().isBefore(earliestExpiry)) {
                        earliestExpiry = seat.getHoldExpiry();
                    }
                }
            } else {
                allValid = false;
            }
        }

        // Build trip details
        BookingPreviewResponse.TripDetails tripDetails = BookingPreviewResponse.TripDetails.builder()
                .tripId(trip.getTripId())
                .routeName(trip.getRoute() != null ? trip.getRoute().getRouteName() : null)
                .departureLocation(trip.getRoute() != null && trip.getRoute().getOrigin() != null 
                        ? trip.getRoute().getOrigin().getLocationName() : null)
                .arrivalLocation(trip.getRoute() != null && trip.getRoute().getDestination() != null 
                        ? trip.getRoute().getDestination().getLocationName() : null)
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .vehicleType(trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null 
                        ? trip.getVehicle().getVehicleType().getTypeName() : null)
                .vehiclePlate(trip.getVehicle() != null ? trip.getVehicle().getLicensePlate() : null)
                .build();

        return BookingPreviewResponse.builder()
                .valid(allValid)
                .message(allValid ? "Tất cả ghế đều hợp lệ để đặt" : "Một số ghế không hợp lệ để đặt")
                .tripId(tripId)
                .tripDetails(tripDetails)
                .seats(seatInfos)
                .totalAmount(totalAmount)
                .lockExpiry(earliestExpiry)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(BookingConfirmRequest request) {
        log.info("Confirming booking for trip {} with seats {} by user {}", 
                request.getTripId(), request.getSeatIds(), request.getUserId());

        // 1. Validate trip exists
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến đi với ID: " + request.getTripId()));

        // 2. Validate and lock all seats with pessimistic locking
        List<TripSeat> seatsToBook = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Integer seatId : request.getSeatIds()) {
            // Use pessimistic lock to prevent concurrent booking
            TripSeat seat = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, request.getTripId())
                    .orElseThrow(() -> new NotFoundException(
                            "Ghế " + seatId + " không tồn tại trong chuyến đi này"));

            // Validate seat lock ownership
            validateSeatOwnership(seat, request.getUserId());

            seatsToBook.add(seat);
            totalAmount = totalAmount.add(trip.getBasePrice());
        }

        // 3. Generate booking code
        String bookingCode = generateBookingCode();

        // 4. Find customer if not guest booking
        User customer = null;
        if (!Boolean.TRUE.equals(request.getIsGuestBooking())) {
            try {
                Integer customerId = Integer.parseInt(request.getUserId());
                customer = userRepository.findById(customerId).orElse(null);
            } catch (NumberFormatException e) {
                // userId is not a number, treat as guest
                log.debug("User ID {} is not a valid customer ID, treating as guest", request.getUserId());
            }
        }

        // 5. Create booking
        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .customer(customer)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .trip(trip)
                .totalAmount(totalAmount)
                .bookingStatus("Paid")
                .bookingType("Online")
                .isGuestBooking(customer == null)
                .guestSessionId(customer == null ? request.getGuestSessionId() : null)
                .build();

        booking = bookingRepository.save(booking);

        // 6. Create tickets and update seat status
        List<Ticket> tickets = new ArrayList<>();
        Map<Integer, BookingConfirmRequest.PassengerInfo> passengerInfoMap = new HashMap<>();
        
        if (request.getPassengers() != null) {
            for (BookingConfirmRequest.PassengerInfo p : request.getPassengers()) {
                passengerInfoMap.put(p.getSeatId(), p);
            }
        }

        for (TripSeat seat : seatsToBook) {
            // Mark seat as booked
            seat.book();
            tripSeatRepository.save(seat);

            // Generate ticket code
            String ticketCode = generateTicketCode();

            // Create ticket
            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(booking)
                    .seat(seat)
                    .price(trip.getBasePrice())
                    .ticketStatus("Confirmed")
                    .build();

            ticket = ticketRepository.save(ticket);
            tickets.add(ticket);

            // Create passenger if info provided
            BookingConfirmRequest.PassengerInfo passengerInfo = passengerInfoMap.get(seat.getSeatId());
            if (passengerInfo != null) {
                createPassenger(ticket, passengerInfo);
            }

            // Broadcast seat status change
            broadcastSeatUpdate(trip.getTripId(), seat);
        }

        log.info("Booking {} confirmed successfully with {} tickets", bookingCode, tickets.size());

        // 7. Build and return response
        return bookingMapper.toBookingResponse(booking, trip, tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));
        
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByCode(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với mã: " + bookingCode));
        
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(Integer customerId) {
        List<Booking> bookings = bookingRepository.findByCustomerId(customerId);
        return bookings.stream()
                .map(booking -> {
                    List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
                    return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByTripId(Integer tripId) {
        List<Booking> bookings = bookingRepository.findByTripId(tripId);
        return bookings.stream()
                .map(booking -> {
                    List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
                    return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, String userId) {
        log.info("Cancelling booking {} by user {}", bookingId, userId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));

        // Validate cancellation permission
        if (booking.getCustomer() != null) {
            try {
                Integer requesterId = Integer.parseInt(userId);
                if (!booking.getCustomer().getUserId().equals(requesterId)) {
                    throw new BadRequestException("Bạn không có quyền hủy booking này");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("Bạn không có quyền hủy booking này");
            }
        }

        // Check if booking can be cancelled
        if ("Cancelled".equals(booking.getBookingStatus())) {
            throw new BadRequestException("Booking đã được hủy trước đó");
        }

        // Update booking status
        booking.setBookingStatus("Cancelled");
        bookingRepository.save(booking);

        // Release all seats
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus("Cancelled");
            ticketRepository.save(ticket);

            TripSeat seat = ticket.getSeat();
            if (seat != null) {
                seat.release();
                tripSeatRepository.save(seat);
                broadcastSeatUpdate(booking.getTrip().getTripId(), seat);
            }
        }

        log.info("Booking {} cancelled successfully", bookingId);
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByPhone(String phone) {
        List<Booking> bookings = bookingRepository.findByCustomerPhone(phone);
        return bookings.stream()
                .map(booking -> {
                    List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
                    return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
                })
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validate if the seat can be booked by this user
     */
    private boolean validateSeatForBooking(TripSeat seat, String userId) {
        // Seat must be locked
        if (!seat.isLocked()) {
            return false;
        }

        // Lock must not be expired
        if (seat.isLockExpired()) {
            return false;
        }

        // Seat must be locked by this user
        if (!userId.equals(seat.getLockedBy())) {
            return false;
        }

        return true;
    }

    /**
     * Get validation message for a seat
     */
    private String getValidationMessage(TripSeat seat, String userId) {
        if (seat.isBooked()) {
            return "Ghế đã được đặt";
        }

        if (seat.isAvailable()) {
            return "Ghế chưa được khóa. Vui lòng khóa ghế trước khi đặt";
        }

        if (!seat.isLocked()) {
            return "Trạng thái ghế không hợp lệ: " + seat.getStatus();
        }

        if (seat.isLockExpired()) {
            return "Thời gian khóa ghế đã hết hạn";
        }

        if (!userId.equals(seat.getLockedBy())) {
            return "Ghế đang được giữ bởi người dùng khác";
        }

        return "Ghế hợp lệ để đặt";
    }

    /**
     * Validate seat ownership before booking - throws exception if invalid
     */
    private void validateSeatOwnership(TripSeat seat, String userId) {
        String seatNumber = seat.getSeatNumber();
        Integer seatId = seat.getSeatId();

        // Check if seat is already booked
        if (seat.isBooked()) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") đã được đặt");
        }

        // Check if seat is locked
        if (!seat.isLocked()) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") chưa được khóa. Vui lòng khóa ghế trước khi đặt");
        }

        // Check if lock has expired
        if (seat.isLockExpired()) {
            throw new BadRequestException("Thời gian khóa ghế " + seatNumber + " (ID: " + seatId + ") đã hết hạn. Vui lòng khóa lại");
        }

        // Check if locked by this user
        if (!userId.equals(seat.getLockedBy())) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") đang được giữ bởi người dùng khác");
        }
    }

    /**
     * Generate unique booking code
     * Format: BK + YYYYMMDD + 3-digit sequence
     */
    private String generateBookingCode() {
        String datePrefix = LocalDate.now().format(DATE_FORMAT);
        Integer latestSequence = bookingRepository.getLatestBookingSequence(datePrefix);
        int nextSequence = (latestSequence != null ? latestSequence : 0) + 1;
        return String.format("BK%s%03d", datePrefix, nextSequence);
    }

    /**
     * Generate unique ticket code
     * Format: TK + YYYYMMDD + 3-digit sequence
     */
    private String generateTicketCode() {
        String datePrefix = LocalDate.now().format(DATE_FORMAT);
        Integer latestSequence = ticketRepository.getLatestTicketSequence(datePrefix);
        int nextSequence = (latestSequence != null ? latestSequence : 0) + 1;
        return String.format("TK%s%03d", datePrefix, nextSequence);
    }

    /**
     * Create passenger record for a ticket
     */
    private void createPassenger(Ticket ticket, BookingConfirmRequest.PassengerInfo info) {
        // Check if passenger already exists for this ticket (prevent duplicate key error)
        Optional<Passenger> existingPassenger = passengerRepository.findByTicket_TicketId(ticket.getTicketId());
        if (existingPassenger.isPresent()) {
            log.warn("Passenger already exists for ticket {}, skipping creation", ticket.getTicketId());
            return;
        }

        RouteStop pickupStop = null;
        RouteStop dropoffStop = null;

        if (info.getPickupStopId() != null) {
            pickupStop = routeStopRepository.findById(info.getPickupStopId()).orElse(null);
        }
        if (info.getDropoffStopId() != null) {
            dropoffStop = routeStopRepository.findById(info.getDropoffStopId()).orElse(null);
        }

        Passenger passenger = Passenger.builder()
                .ticket(ticket)
                .fullName(info.getFullName())
                .phoneNumber(info.getPhoneNumber())
                .pickupLocation(pickupStop)
                .pickupAddress(info.getPickupAddress())
                .dropoffLocation(dropoffStop)
                .dropoffAddress(info.getDropoffAddress())
                .specialNote(info.getSpecialNote())
                .build();

        passengerRepository.save(passenger);
    }

    /**
     * Broadcast seat status update via WebSocket
     */
    private void broadcastSeatUpdate(Integer tripId, TripSeat seat) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("seatId", seat.getSeatId());
            message.put("seatNumber", seat.getSeatNumber());
            message.put("status", seat.getStatus());
            message.put("lockedBy", seat.getLockedBy());
            message.put("holdExpiry", seat.getHoldExpiry());
            message.put("timestamp", LocalDateTime.now().toString());

            String destination = "/topic/trips/" + tripId + "/seats";
            messagingTemplate.convertAndSend(destination, (Object) message);
            log.debug("Broadcast seat update for seat {} on trip {}", seat.getSeatId(), tripId);
        } catch (Exception e) {
            log.error("Failed to broadcast seat update: {}", e.getMessage());
        }
    }
}
