package com.example.Fuba_BE.service.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Refund;
import com.example.Fuba_BE.domain.entity.RouteStop;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.domain.enums.TicketStatus;
import com.example.Fuba_BE.dto.Booking.BookingConfirmRequest;
import com.example.Fuba_BE.dto.Booking.BookingFilterRequest;
import com.example.Fuba_BE.dto.Booking.BookingPageResponse;
import com.example.Fuba_BE.dto.Booking.BookingPreviewResponse;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.dto.Booking.CounterBookingRequest;
import com.example.Fuba_BE.dto.Booking.RescheduleRequest;
import com.example.Fuba_BE.dto.Booking.RescheduleResponse;
import com.example.Fuba_BE.dto.Booking.TicketCountResponse;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.BookingMapper;
import com.example.Fuba_BE.repository.BookingRepository;
import com.example.Fuba_BE.repository.PassengerRepository;
import com.example.Fuba_BE.repository.RefundRepository;
import com.example.Fuba_BE.repository.RouteStopRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;
import com.example.Fuba_BE.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final RefundRepository refundRepository;
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
                        ? trip.getRoute().getOrigin().getLocationName()
                        : null)
                .arrivalLocation(trip.getRoute() != null && trip.getRoute().getDestination() != null
                        ? trip.getRoute().getDestination().getLocationName()
                        : null)
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .vehicleType(trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null
                        ? trip.getVehicle().getVehicleType().getTypeName()
                        : null)
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
                    .ticketStatus(TicketStatus.UNCONFIRMED.getDisplayName())
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
                    .ticketStatus(TicketStatus.CONFIRMED.getDisplayName())
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
            throw new BadRequestException(
                    "Booking không ở trạng thái Held hoặc Pending. Hiện tại: " + booking.getBookingStatus());
        }

        // TODO: Integrate actual payment gateway here

        booking.setBookingStatus("Paid");
        booking = bookingRepository.save(booking);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus(TicketStatus.CONFIRMED.getDisplayName());
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
    public BookingResponse getBookingByTicketCode(String ticketCode) {
        Booking booking = bookingRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với mã vé: " + ticketCode));

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
                throw new BadRequestException(
                        "Invalid booking status. Valid values: Held, Pending, Paid, Cancelled, Expired, Completed");
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
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByEmail(String email) {
        List<Booking> bookings = bookingRepository.findByCustomerEmail(email);
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
                            ". Chỉ có thể hủy booking ở trạng thái: Held, Pending, Paid");
        }

        String previousStatus = booking.getBookingStatus();
        boolean needsRefund = "Paid".equals(previousStatus);

        booking.setBookingStatus("Cancelled");
        bookingRepository.save(booking);

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        List<Integer> ticketIds = new ArrayList<>();

        for (Ticket ticket : tickets) {
            ticket.setTicketStatus(TicketStatus.CANCELLED.getDisplayName());
            ticketRepository.save(ticket);
            ticketIds.add(ticket.getTicketId());

            TripSeat seat = ticket.getSeat();
            if (seat != null) {
                seat.release();
                tripSeatRepository.save(seat);
                broadcastSeatUpdate(booking.getTrip().getTripId(), seat);
            }
        }

        // Calculate refund based on cancellation policy
        if (needsRefund && booking.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            LocalDateTime departureTime = booking.getTrip().getDepartureTime();
            LocalDateTime now = LocalDateTime.now();
            long hoursUntilDeparture = java.time.Duration.between(now, departureTime).toHours();

            BigDecimal refundAmount;
            int refundPercentage;
            String refundType;
            String refundReason;

            if (hoursUntilDeparture >= 48) {
                // Cancel 2+ days before → 100% refund
                refundPercentage = 100;
                refundAmount = booking.getTotalAmount();
                refundType = Refund.TYPE_FULL_CANCELLATION;
                refundReason = "Khách hàng hủy vé trước 2 ngày - Hoàn 100%";
            } else if (hoursUntilDeparture >= 12) {
                // Cancel 12h - 2 days before → 50% refund
                refundPercentage = 50;
                refundAmount = booking.getTotalAmount()
                        .multiply(BigDecimal.valueOf(50))
                        .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                refundType = Refund.TYPE_PARTIAL_CANCELLATION;
                refundReason = "Khách hàng hủy vé trong vòng 12 tiếng - 2 ngày trước khởi hành - Hoàn 50%";
            } else {
                // Cancel less than 12h before → No refund
                refundPercentage = 0;
                refundAmount = BigDecimal.ZERO;
                refundType = null; // No refund record needed
                refundReason = "Khách hàng hủy vé trong vòng 12 tiếng trước khởi hành - Không hoàn tiền";
            }

            // Only create refund record if there's money to refund
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                Refund refund = Refund.builder()
                        .booking(booking)
                        .refundAmount(refundAmount)
                        .refundReason(refundReason)
                        .refundType(refundType)
                        .affectedTicketIds(ticketIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .refundStatus(Refund.STATUS_REFUNDED) // Mock: auto refunded
                        .refundMethod(Refund.METHOD_TRANSFER) // Default: Transfer
                        .build();
                refundRepository.save(refund);

                log.info("Created refund for booking {}. Amount: {} ({}% of {})",
                        booking.getBookingCode(), refundAmount, refundPercentage, booking.getTotalAmount());
            } else {
                log.info("No refund for booking {} - cancelled less than 12 hours before departure",
                        booking.getBookingCode());
            }
        }

        log.info("Booking {} cancelled successfully by user. {} seats released. Refund needed: {}",
                booking.getBookingCode(), tickets.size(), needsRefund);
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    @Override
    @Transactional
    public RescheduleResponse rescheduleBooking(RescheduleRequest request) {
        log.info("Rescheduling booking {} to trip {} by user {}",
                request.getOldBookingId(), request.getNewTripId(), request.getUserId());

        // 1. Validate old booking
        Booking oldBooking = bookingRepository.findByIdWithLock(request.getOldBookingId())
                .orElseThrow(
                        () -> new NotFoundException("Không tìm thấy booking với ID: " + request.getOldBookingId()));

        // Check ownership
        if (oldBooking.getCustomer() != null) {
            try {
                Integer requesterId = Integer.parseInt(request.getUserId());
                if (!oldBooking.getCustomer().getUserId().equals(requesterId)) {
                    throw new BadRequestException("Bạn không có quyền đổi vé booking này");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("Bạn không có quyền đổi vé booking này");
            }
        }

        // Check status - only Paid bookings can be rescheduled
        if (!"Paid".equals(oldBooking.getBookingStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể đổi vé đã thanh toán. Trạng thái hiện tại: " + oldBooking.getBookingStatus());
        }

        // Check reschedule time - must be at least 12 hours before departure
        LocalDateTime oldDepartureTime = oldBooking.getTrip().getDepartureTime();
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilDeparture = java.time.Duration.between(now, oldDepartureTime).toHours();

        if (hoursUntilDeparture < 12) {
            throw new BadRequestException("Không thể đổi vé trong vòng 12 tiếng trước giờ khởi hành");
        }

        // 2. Validate new trip
        Trip newTrip = tripRepository.findById(request.getNewTripId())
                .orElseThrow(
                        () -> new NotFoundException("Không tìm thấy chuyến đi mới với ID: " + request.getNewTripId()));

        // Check new trip is not the same as old trip
        if (newTrip.getTripId().equals(oldBooking.getTrip().getTripId())) {
            throw new BadRequestException("Chuyến đi mới phải khác chuyến đi cũ");
        }

        // Check new trip departure is in the future
        if (newTrip.getDepartureTime().isBefore(now)) {
            throw new BadRequestException("Chuyến đi mới đã khởi hành");
        }

        // Check new trip status
        if ("Cancelled".equals(newTrip.getStatus()) || "COMPLETED".equals(newTrip.getStatus())) {
            throw new BadRequestException("Chuyến đi mới không khả dụng. Trạng thái: " + newTrip.getStatus());
        }

        // 3. Validate new seats
        List<TripSeat> newSeats = new ArrayList<>();
        for (Integer seatId : request.getNewSeatIds()) {
            TripSeat seat = tripSeatRepository.findBySeatIdAndTripId(seatId, request.getNewTripId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy ghế " + seatId + " trong chuyến đi mới"));

            // Seat must be locked by this user or available
            if (seat.isBooked()) {
                throw new BadRequestException("Ghế " + seat.getSeatNumber() + " đã được đặt");
            }
            if (seat.isLocked() && !request.getUserId().equals(seat.getLockedBy())) {
                throw new BadRequestException("Ghế " + seat.getSeatNumber() + " đang được giữ bởi người khác");
            }
            newSeats.add(seat);
        }

        // 4. Calculate financial details
        BigDecimal oldAmount = oldBooking.getTotalAmount();
        BigDecimal newAmount = newTrip.getBasePrice().multiply(BigDecimal.valueOf(newSeats.size()));
        BigDecimal priceDifference = newAmount.subtract(oldAmount);

        BigDecimal refundAmount = BigDecimal.ZERO;
        BigDecimal extraFee = BigDecimal.ZERO;
        String financialDescription;

        if (priceDifference.compareTo(BigDecimal.ZERO) < 0) {
            // New trip is cheaper -> refund the difference
            refundAmount = priceDifference.abs();
            financialDescription = String.format("Hoàn tiền %s VNĐ do chuyến mới rẻ hơn", refundAmount);
        } else if (priceDifference.compareTo(BigDecimal.ZERO) > 0) {
            // New trip is more expensive -> customer pays extra
            extraFee = priceDifference;
            financialDescription = String.format("Phụ thu %s VNĐ do chuyến mới đắt hơn", extraFee);
        } else {
            financialDescription = "Không phát sinh chi phí thêm";
        }

        // 5. Cancel old booking (release seats)
        List<Ticket> oldTickets = ticketRepository.findByBookingId(oldBooking.getBookingId());
        List<Integer> oldTicketIds = new ArrayList<>();

        for (Ticket ticket : oldTickets) {
            ticket.setTicketStatus(TicketStatus.RESCHEDULED.getDisplayName());
            ticketRepository.save(ticket);
            oldTicketIds.add(ticket.getTicketId());

            TripSeat oldSeat = ticket.getSeat();
            if (oldSeat != null) {
                oldSeat.release();
                tripSeatRepository.save(oldSeat);
                broadcastSeatUpdate(oldBooking.getTrip().getTripId(), oldSeat);
            }
        }

        oldBooking.setBookingStatus("Rescheduled");
        bookingRepository.save(oldBooking);

        // 6. Create new booking
        String newBookingCode = generateBookingCode();

        Booking newBooking = Booking.builder()
                .bookingCode(newBookingCode)
                .customer(oldBooking.getCustomer())
                .customerName(oldBooking.getCustomerName())
                .customerPhone(oldBooking.getCustomerPhone())
                .customerEmail(oldBooking.getCustomerEmail())
                .trip(newTrip)
                .totalAmount(newAmount)
                .bookingStatus("Paid") // Auto-paid for reschedule
                .bookingType(oldBooking.getBookingType())
                .isGuestBooking(oldBooking.getIsGuestBooking())
                .guestSessionId(oldBooking.getGuestSessionId())
                .holdExpiry(null) // No hold for rescheduled booking
                .build();

        newBooking = bookingRepository.save(newBooking);

        // 7. Create new tickets
        List<Ticket> newTickets = new ArrayList<>();
        int passengerIndex = 0;

        for (TripSeat newSeat : newSeats) {
            String ticketCode = generateTicketCode();

            Ticket newTicket = Ticket.builder()
                    .ticketCode(ticketCode)
                    .booking(newBooking)
                    .seat(newSeat)
                    .price(newTrip.getBasePrice())
                    .ticketStatus(TicketStatus.CONFIRMED.getDisplayName())
                    .build();
            newTicket = ticketRepository.save(newTicket);
            newTickets.add(newTicket);

            // Update seat status
            newSeat.setStatus("Booked");
            newSeat.setLockedBy(null);
            newSeat.setHoldExpiry(null);
            tripSeatRepository.save(newSeat);
            broadcastSeatUpdate(newTrip.getTripId(), newSeat);

            // Create passenger info
            if (request.getPassengers() != null && passengerIndex < request.getPassengers().size()) {
                createPassenger(newTicket, request.getPassengers().get(passengerIndex));
            } else if (passengerIndex < oldTickets.size()) {
                // Copy passenger from old ticket
                Optional<Passenger> oldPassenger = passengerRepository.findByTicket_TicketId(
                        oldTickets.get(passengerIndex).getTicketId());
                if (oldPassenger.isPresent()) {
                    Passenger op = oldPassenger.get();
                    Passenger newPassenger = Passenger.builder()
                            .ticket(newTicket)
                            .fullName(op.getFullName())
                            .phoneNumber(op.getPhoneNumber())
                            .pickupLocation(op.getPickupLocation())
                            .pickupAddress(op.getPickupAddress())
                            .dropoffLocation(op.getDropoffLocation())
                            .dropoffAddress(op.getDropoffAddress())
                            .specialNote(op.getSpecialNote())
                            .build();
                    passengerRepository.save(newPassenger);
                }
            }
            passengerIndex++;
        }

        // 8. Create refund record if applicable
        Integer refundId = null;
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Refund refund = Refund.builder()
                    .booking(oldBooking)
                    .refundAmount(refundAmount)
                    .refundReason(request.getReason() != null ? request.getReason() : "Đổi vé sang chuyến rẻ hơn")
                    .refundType(Refund.TYPE_RESCHEDULE)
                    .affectedTicketIds(oldTicketIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .newTrip(newTrip)
                    .priceDifference(priceDifference)
                    .refundStatus(Refund.STATUS_REFUNDED) // Mock: auto refunded
                    .refundMethod(Refund.METHOD_TRANSFER)
                    .build();
            refund = refundRepository.save(refund);
            refundId = refund.getRefundId();

            log.info("Created refund {} for reschedule. Amount: {}", refundId, refundAmount);
        }

        // 9. Build response
        BookingResponse oldBookingResponse = bookingMapper.toBookingResponse(oldBooking, oldBooking.getTrip(),
                oldTickets);
        BookingResponse newBookingResponse = bookingMapper.toBookingResponse(newBooking, newTrip, newTickets);

        RescheduleResponse.FinancialSummary financialSummary = RescheduleResponse.FinancialSummary.builder()
                .oldBookingAmount(oldAmount)
                .newBookingAmount(newAmount)
                .priceDifference(priceDifference)
                .refundAmount(refundAmount)
                .extraFee(extraFee)
                .rescheduleFee(BigDecimal.ZERO) // No reschedule fee for now
                .netAmount(priceDifference)
                .description(financialDescription)
                .build();

        String message = refundAmount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Đổi vé thành công! Hoàn tiền: %s VNĐ", refundAmount)
                : extraFee.compareTo(BigDecimal.ZERO) > 0
                        ? String.format("Đổi vé thành công! Phụ thu: %s VNĐ", extraFee)
                        : "Đổi vé thành công!";

        log.info("Reschedule completed: {} -> {}. Price diff: {}",
                oldBooking.getBookingCode(), newBooking.getBookingCode(), priceDifference);

        return RescheduleResponse.builder()
                .oldBooking(oldBookingResponse)
                .newBooking(newBookingResponse)
                .financialSummary(financialSummary)
                .refundId(refundId)
                .message(message)
                .build();
    }

    // ==================== Private Helper Methods ====================

    private boolean validateSeatForBooking(TripSeat seat, String userId) {
        if (!seat.isLocked())
            return false;
        if (seat.isLockExpired())
            return false;
        return userId.equals(seat.getLockedBy());
    }

    private String getValidationMessage(TripSeat seat, String userId) {
        if (seat.isBooked())
            return "Ghế đã được đặt";
        if (seat.isAvailable())
            return "Ghế chưa được khóa. Vui lòng khóa ghế trước khi đặt";
        if (!seat.isLocked())
            return "Trạng thái ghế không hợp lệ: " + seat.getStatus();
        if (seat.isLockExpired())
            return "Thời gian khóa ghế đã hết hạn";
        if (!userId.equals(seat.getLockedBy()))
            return "Ghế đang được giữ bởi người dùng khác";
        return "Ghế hợp lệ để đặt";
    }

    private void validateSeatOwnership(TripSeat seat, String userId) {
        String seatNumber = seat.getSeatNumber();
        Integer seatId = seat.getSeatId();

        if (seat.isBooked()) {
            throw new BadRequestException("Ghế " + seatNumber + " (ID: " + seatId + ") đã được đặt");
        }
        if (!seat.isLocked()) {
            throw new BadRequestException(
                    "Ghế " + seatNumber + " (ID: " + seatId + ") chưa được khóa. Vui lòng khóa ghế trước khi đặt");
        }
        if (seat.isLockExpired()) {
            throw new BadRequestException(
                    "Thời gian khóa ghế " + seatNumber + " (ID: " + seatId + ") đã hết hạn. Vui lòng khóa lại");
        }
        if (!userId.equals(seat.getLockedBy())) {
            throw new BadRequestException(
                    "Ghế " + seatNumber + " (ID: " + seatId + ") đang được giữ bởi người dùng khác");
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

    @Override
    @Transactional(readOnly = true)
    public BookingPageResponse getAllBookings(BookingFilterRequest filterRequest) {
        log.info("Getting all bookings with filters: {}", filterRequest);

        // Build sort
        Sort sort = Sort.by(
                "DESC".equalsIgnoreCase(filterRequest.getSortDirection())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                filterRequest.getSortBy());

        // Build pageable
        Pageable pageable = PageRequest.of(
                filterRequest.getPage(),
                filterRequest.getSize(),
                sort);

        // Query with filters
        Page<Booking> bookingPage = bookingRepository.findAllWithFilters(
                filterRequest.getStatus(),
                filterRequest.getSearch(),
                pageable);

        // Map to response
        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(booking -> {
                    List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
                    return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
                })
                .collect(Collectors.toList());

        return BookingPageResponse.builder()
                .bookings(bookingResponses)
                .currentPage(bookingPage.getNumber())
                .pageSize(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .isFirst(bookingPage.isFirst())
                .isLast(bookingPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse confirmBookingById(Integer bookingId) {
        log.info("Confirming booking with ID: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy booking với ID: " + bookingId));

        // Validate current status
        if (!"Pending".equals(booking.getBookingStatus()) && !"Held".equals(booking.getBookingStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận booking ở trạng thái Pending hoặc Held. Trạng thái hiện tại: "
                            + booking.getBookingStatus());
        }

        // Update status
        booking.setBookingStatus("Paid");
        bookingRepository.save(booking);

        // Update all tickets
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
        for (Ticket ticket : tickets) {
            ticket.setTicketStatus("Confirmed");
        }
        ticketRepository.saveAll(tickets);

        // Update seats to Booked status
        for (Ticket ticket : tickets) {
            TripSeat tripSeat = tripSeatRepository.findBySeatIdAndTripId(
                    ticket.getSeat().getSeatId(),
                    booking.getTrip().getTripId()).orElse(null);

            if (tripSeat != null) {
                tripSeat.setStatus("Booked");
                tripSeat.setLockedBy(null);
                tripSeat.setHoldExpiry(null);
                tripSeatRepository.save(tripSeat);

                // Broadcast update
                broadcastSeatUpdate(booking.getTrip().getTripId(), tripSeat);
            }
        }

        log.info("Booking {} confirmed successfully", bookingId);
        return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingPageResponse getMyTickets(Integer userId, String status, Integer page, Integer size) {
        log.info("Get my tickets for user {}: status={}, page={}, size={}", userId, status, page, size);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Build pageable with sort by departure time descending
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "trip.departureTime"));

        Page<Booking> bookingPage;

        if (status == null || status.trim().isEmpty()) {
            // Get all bookings
            bookingPage = bookingRepository.findByCustomer(user, pageable);
        } else if ("Upcoming".equalsIgnoreCase(status)) {
            // Upcoming = Held + Paid (not completed, not cancelled, departure time in
            // future)
            LocalDateTime now = LocalDateTime.now();
            bookingPage = bookingRepository.findByCustomerAndBookingStatusInAndTripDepartureTimeAfter(
                    user, Arrays.asList("Held", "Paid"), now, pageable);
        } else if ("Completed".equalsIgnoreCase(status)) {
            bookingPage = bookingRepository.findByCustomerAndBookingStatus(user, "Completed", pageable);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            bookingPage = bookingRepository.findByCustomerAndBookingStatus(user, "Cancelled", pageable);
        } else {
            // Exact status match
            bookingPage = bookingRepository.findByCustomerAndBookingStatus(user, status, pageable);
        }

        List<BookingResponse> bookingResponses = bookingPage.getContent().stream()
                .map(booking -> {
                    List<Ticket> tickets = ticketRepository.findByBookingId(booking.getBookingId());
                    return bookingMapper.toBookingResponse(booking, booking.getTrip(), tickets);
                })
                .collect(Collectors.toList());

        return BookingPageResponse.builder()
                .bookings(bookingResponses)
                .currentPage(bookingPage.getNumber())
                .pageSize(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .isFirst(bookingPage.isFirst())
                .isLast(bookingPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketCountResponse getMyTicketsCount(Integer userId) {
        log.info("Get my tickets count for user {}", userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + userId));

        LocalDateTime now = LocalDateTime.now();

        // Count upcoming (Held + Paid with future departure)
        Long upcomingCount = bookingRepository.countByCustomerAndBookingStatusInAndTripDepartureTimeAfter(
                user, Arrays.asList("Held", "Paid"), now);

        // Count completed
        Long completedCount = bookingRepository.countByCustomerAndBookingStatus(user, "Completed");

        // Count cancelled
        Long cancelledCount = bookingRepository.countByCustomerAndBookingStatus(user, "Cancelled");

        // Total
        Long totalCount = bookingRepository.countByCustomer(user);

        return TicketCountResponse.builder()
                .upcomingCount(upcomingCount)
                .completedCount(completedCount)
                .cancelledCount(cancelledCount)
                .totalCount(totalCount)
                .build();
    }
}