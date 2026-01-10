package com.example.Fuba_BE.service.Booking;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.dto.Booking.CounterBookingRequest;
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
 * Implementation of IBookingService.
 * Handles ticket booking with seat lock validation and real-time broadcast.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService implements IBookingService {

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

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến đi với ID: " + request.getTripId()));

        List<TripSeat> seatsToBook = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Integer seatId : request.getSeatIds()) {
            TripSeat seat = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, request.getTripId())
                    .orElseThrow(() -> new NotFoundException(
                            "Ghế " + seatId + " không tồn tại trong chuyến đi này"));

            validateSeatOwnership(seat, request.getUserId());

            seatsToBook.add(seat);
            totalAmount = totalAmount.add(trip.getBasePrice());
        }

        String bookingCode = generateBookingCode();

        User customer = null;
        if (!Boolean.TRUE.equals(request.getIsGuestBooking())) {
            try {
                Integer customerId = Integer.parseInt(request.getUserId());
                customer = userRepository.findById(customerId).orElse(null);
            } catch (NumberFormatException e) {
                log.debug("User ID {} is not a valid customer ID, treating as guest", request.getUserId());
            }
        }

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .customer(customer)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .trip(trip)
                .totalAmount(totalAmount)
                .bookingStatus("Held")
                .bookingType("Online")
                .isGuestBooking(customer == null)
                .guestSessionId(customer == null ? request.getGuestSessionId() : null)
                .holdExpiry(LocalDateTime.now().plusMinutes(15))
                .build();

        booking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        Map<Integer, BookingConfirmRequest.PassengerInfo> passengerInfoMap = new HashMap<>();

        if (request.getPassengers() != null) {
            for (BookingConfirmRequest.PassengerInfo p : request.getPassengers()) {
                passengerInfoMap.put(p.getSeatId(), p);
            }
        }

        for (TripSeat seat : seatsToBook) {
            // Seat status remains "Held" until payment
            String ticketCode = generateTicketCode();

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(booking)
                    .seat(seat)
                    .price(trip.getBasePrice())
                    .ticketStatus("Unconfirmed")
                    .build();

            ticket = ticketRepository.save(ticket);
            tickets.add(ticket);

            BookingConfirmRequest.PassengerInfo passengerInfo = passengerInfoMap.get(seat.getSeatId());
            if (passengerInfo != null) {
                createPassenger(ticket, passengerInfo);
            }

            broadcastSeatUpdate(trip.getTripId(), seat);
        }

        log.info("Booking {} created with Held status and {} tickets", bookingCode, tickets.size());
        return bookingMapper.toBookingResponse(booking, trip, tickets);
    }

    @Override
    @Transactional
    public BookingResponse createCounterBooking(CounterBookingRequest request) {
        log.info("Creating counter booking for trip {} with seats {} by staff {}",
                request.getTripId(), request.getSeatIds(), request.getStaffUserId());

        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chuyến đi với ID: " + request.getTripId()));

        List<TripSeat> seatsToBook = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Integer seatId : request.getSeatIds()) {
            TripSeat seat = tripSeatRepository.findBySeatIdAndTripIdWithLock(seatId, request.getTripId())
                    .orElseThrow(() -> new NotFoundException(
                            "Ghế " + seatId + " không tồn tại trong chuyến đi này"));

            if (seat.isBooked()) {
                throw new BadRequestException(
                        "Ghế " + seat.getSeatNumber() + " (ID: " + seatId + ") đã được đặt");
            }

            seatsToBook.add(seat);
            totalAmount = totalAmount.add(trip.getBasePrice());
        }

        String bookingCode = generateBookingCode();

        User staffUser = null;
        try {
            Integer staffId = Integer.parseInt(request.getStaffUserId());
            staffUser = userRepository.findById(staffId).orElse(null);
        } catch (NumberFormatException e) {
            log.warn("Invalid staff user ID: {}", request.getStaffUserId());
        }

        Booking booking = Booking.builder()
                .bookingCode(bookingCode)
                .customer(null)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .trip(trip)
                .totalAmount(totalAmount)
                .bookingStatus("Paid")
                .bookingType("Counter")
                .isGuestBooking(true)
                .createdBy(staffUser)
                .holdExpiry(LocalDateTime.now())
                .build();

        booking = bookingRepository.save(booking);

        List<Ticket> tickets = new ArrayList<>();
        Map<Integer, CounterBookingRequest.PassengerInfo> passengerInfoMap = new HashMap<>();

        if (request.getPassengers() != null) {
            for (CounterBookingRequest.PassengerInfo p : request.getPassengers()) {
                passengerInfoMap.put(p.getSeatId(), p);
            }
        }

        for (TripSeat seat : seatsToBook) {
            seat.book();
            tripSeatRepository.save(seat);

            String ticketCode = generateTicketCode();

            Ticket ticket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(booking)
                    .seat(seat)
                    .price(trip.getBasePrice())
                    .ticketStatus("Confirmed")
                    .build();

            ticket = ticketRepository.save(ticket);
            tickets.add(ticket);

            CounterBookingRequest.PassengerInfo passengerInfo = passengerInfoMap.get(seat.getSeatId());
            if (passengerInfo != null) {
                createPassengerForCounter(ticket, passengerInfo);
            }

            broadcastSeatUpdate(trip.getTripId(), seat);
        }

        log.info("Counter booking {} created successfully with {} tickets", bookingCode, tickets.size());
        return bookingMapper.toBookingResponse(booking, trip, tickets);
    }

    @Override
    @Transactional
    public BookingResponse processPayment(Integer bookingId, Map<String, Object> paymentDetails) {
        log.info("Processing payment for booking ID: {}", bookingId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));

        if (!"Held".equals(booking.getBookingStatus()) && !"Pending".equals(booking.getBookingStatus())) {
            throw new BadRequestException("Booking không ở trạng thái Held hoặc Pending. Hiện tại: " + booking.getBookingStatus());
        }

        // TODO: Integrate actual payment gateway here

        booking.setBookingStatus("Paid");
        booking = bookingRepository.save(booking);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus("Confirmed");
            ticketRepository.save(ticket);

            TripSeat seat = ticket.getSeat();
            if (seat != null) {
                if (!"Booked".equals(seat.getStatus())) {
                    seat.book();
                    tripSeatRepository.save(seat);
                }
                broadcastSeatUpdate(booking.getTrip().getTripId(), seat);
            }
        }

        log.info("Payment processed successfully for booking {}", booking.getBookingCode());
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
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
        return getBookingsByCustomerId(customerId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomerId(Integer customerId, String status) {
        List<Booking> bookings;
        if (status != null && !status.trim().isEmpty()) {
            List<String> validStatuses = Arrays.asList("Held", "Pending", "Paid", "Cancelled", "Expired", "Completed");
            if (!validStatuses.contains(status)) {
                throw new BadRequestException("Invalid booking status. Valid values: Held, Pending, Paid, Cancelled, Expired, Completed");
            }
            bookings = bookingRepository.findByCustomerIdAndBookingStatus(customerId, status);
        } else {
            bookings = bookingRepository.findByCustomerId(customerId);
        }

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

    @Override
    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, String userId) {
        log.info("Cancelling booking {} by user {}", bookingId, userId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));

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

        List<String> cancellableStatuses = Arrays.asList("Held", "Pending", "Paid");
        if (!cancellableStatuses.contains(booking.getBookingStatus())) {
            throw new BadRequestException(
                    "Không thể hủy booking ở trạng thái: " + booking.getBookingStatus() +
                            ". Chỉ có thể hủy booking ở trạng thái: Held, Pending, Paid"
            );
        }

        booking.setBookingStatus("Cancelled");
        bookingRepository.save(booking);

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

        log.info("Booking {} cancelled successfully by user. {} seats released.",
                booking.getBookingCode(), tickets.size());
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    // ==================== Private Helper Methods ====================

    private boolean validateSeatForBooking(TripSeat seat, String userId) {
        if (!seat.isLocked()) return false;
        if (seat.isLockExpired()) return false;
        return userId.equals(seat.getLockedBy());
    }

    private String getValidationMessage(TripSeat seat, String userId) {
        if (seat.isBooked()) return "Ghế đã được đặt";
        if (seat.isAvailable()) return "Ghế chưa được khóa. Vui lòng khóa ghế trước khi đặt";
        if (!seat.isLocked()) return "Trạng thái ghế không hợp lệ: " + seat.getStatus();
        if (seat.isLockExpired()) return "Thời gian khóa ghế đã hết hạn";
        if (!userId.equals(seat.getLockedBy())) return "Ghế đang được giữ bởi người dùng khác";
        return "Ghế hợp lệ để đặt";
    }

    private void validateSeatOwnership(TripSeat seat, String userId) {
        String seatNumber = seat.getSeatNumber();
        Integer seatId = seat.getSeatId();

        if (seat.isBooked()) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") đã được đặt");
        }
        if (!seat.isLocked()) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") chưa được khóa. Vui lòng khóa ghế trước khi đặt");
        }
        if (seat.isLockExpired()) {
            throw new BadRequestException("Thời gian khóa ghế " + seatNumber + " (ID: " + seatId + ") đã hết hạn. Vui lòng khóa lại");
        }
        if (!userId.equals(seat.getLockedBy())) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") đang được giữ bởi người dùng khác");
        }
    }

    private String generateBookingCode() {
        String datePrefix = LocalDate.now().format(DATE_FORMAT);
        Integer latestSequence = bookingRepository.getLatestBookingSequence(datePrefix);
        int nextSequence = (latestSequence != null ? latestSequence : 0) + 1;
        return String.format("BK%s%03d", datePrefix, nextSequence);
    }

    private String generateTicketCode() {
        String datePrefix = LocalDate.now().format(DATE_FORMAT);
        Integer latestSequence = ticketRepository.getLatestTicketSequence(datePrefix);
        int nextSequence = (latestSequence != null ? latestSequence : 0) + 1;
        return String.format("TK%s%03d", datePrefix, nextSequence);
    }

    private void createPassenger(Ticket ticket, BookingConfirmRequest.PassengerInfo info) {
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

    private void createPassengerForCounter(Ticket ticket, CounterBookingRequest.PassengerInfo info) {
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
                .fullName(info.getPassengerName())
                .phoneNumber(info.getPassengerPhone())
                .pickupLocation(pickupStop)
                .dropoffLocation(dropoffStop)
                .build();

        passengerRepository.save(passenger);
    }

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