package com.example.Fuba_BE.service.Trip;

import java.math.BigDecimal; // [NEW] Cần import để ép kiểu so sánh giá
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.Fuba_BE.domain.entity.Booking;
import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.Passenger;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Ticket;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.domain.enums.SeatStatus;
import com.example.Fuba_BE.domain.enums.TicketStatus;
import com.example.Fuba_BE.domain.enums.TripStatus;
import com.example.Fuba_BE.dto.Trip.CompleteTripRequestDTO;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TicketDetailResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripUpdateRequestDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.PassengerOnTripMapper;
import com.example.Fuba_BE.mapper.TripMapper;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.PassengerRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TicketRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;
import com.example.Fuba_BE.repository.VehicleRepository;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService implements ITripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripSeatRepository tripSeatRepository;
    private final TicketRepository ticketRepository;
    private final PassengerRepository passengerRepository;
    private final PassengerOnTripMapper passengerOnTripMapper;
    private final TripMapper tripMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TripDetailedResponseDTO> getAllTrips(int page, int size, String sortBy, String sortDir,
            String search, Integer originId, Integer destId,
            Double minPrice, Double maxPrice, LocalDate date,
            List<String> timeRanges, List<String> vehicleTypes,
            Integer minAvailableSeats) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Trip> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Search (Route Name)
            if (search != null && !search.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("routeName")), "%" + search.toLowerCase() + "%"));
            }
            if (originId != null) {
                predicates.add(cb.equal(root.get("route").get("origin").get("id"), originId));
            }
            if (destId != null) {
                predicates.add(cb.equal(root.get("route").get("destination").get("id"), destId));
            }

            // 2. Price Range [FIXED]: Sửa "price" thành "basePrice" và ép kiểu BigDecimal
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(minPrice)));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(maxPrice)));
            }

            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
                predicates.add(cb.between(root.get("departureTime"), startOfDay, endOfDay));
            }

            // 3. Time Ranges [FIXED]: Sửa cho PostgreSQL (Supabase) dùng date_part
            if (timeRanges != null && !timeRanges.isEmpty()) {
                List<Predicate> timePredicates = new ArrayList<>();

                // Fix: Dùng date_part thay vì function("hour")
                Expression<Double> hourDouble = cb.function("date_part", Double.class, cb.literal("hour"),
                        root.get("departureTime"));
                Expression<Integer> hourExp = hourDouble.as(Integer.class);

                for (String range : timeRanges) {
                    switch (range.toLowerCase()) {
                        case "morning":
                            timePredicates.add(cb.and(cb.greaterThanOrEqualTo(hourExp, 6), cb.lessThan(hourExp, 12)));
                            break;
                        case "afternoon":
                            timePredicates.add(cb.and(cb.greaterThanOrEqualTo(hourExp, 12), cb.lessThan(hourExp, 18)));
                            break;
                        case "evening":
                            timePredicates.add(cb.and(cb.greaterThanOrEqualTo(hourExp, 18), cb.lessThan(hourExp, 24)));
                            break;
                        case "night":
                            timePredicates.add(cb.and(cb.greaterThanOrEqualTo(hourExp, 0), cb.lessThan(hourExp, 6)));
                            break;
                    }
                }
                if (!timePredicates.isEmpty())
                    predicates.add(cb.or(timePredicates.toArray(new Predicate[0])));
            }

            // 4. Vehicle Types
            if (vehicleTypes != null && !vehicleTypes.isEmpty()) {
                predicates.add(root.get("vehicle").get("vehicleType").get("typeName").in(vehicleTypes));
            }

            // 5. Min Available Seats (Subquery)
            if (minAvailableSeats != null && minAvailableSeats > 0) {
                Subquery<Long> sub = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<TripSeat> subRoot = sub.from(TripSeat.class);

                sub.select(cb.count(subRoot));
                sub.where(
                        cb.equal(subRoot.get("trip"), root),
                        cb.equal(subRoot.get("status"), SeatStatus.Available.getDisplayName()));
                predicates.add(cb.greaterThanOrEqualTo(sub, minAvailableSeats.longValue()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Trip> tripPage = tripRepository.findAll(spec, pageable);
        return tripPage.map(trip -> {
            TripDetailedResponseDTO dto = tripMapper.toDetailedDTO(trip);

            if (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null) {
                dto.setTotalSeats(trip.getVehicle().getVehicleType().getTotalSeats());
            } else {
                dto.setTotalSeats(40); // Default fallback
            }

            return this.enrichTripStats(dto, trip.getTripId());
        });
    }

    // ... CÁC HÀM KHÁC GIỮ NGUYÊN NHƯ CŨ ...

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId,
            Pageable pageable) {
        String filterStatus = StringUtils.hasText(status) ? status : null;
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (date != null) {
            start = date.atStartOfDay();
            end = date.atTime(LocalTime.MAX);
        }
        return tripRepository.findTripsWithFilter(filterStatus, start, end, originId, destId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate))
            throw new BadRequestException("Start date cannot be after end date");
        return tripRepository.findDistinctTripDates(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> getTripsDetailsByDate(LocalDate date) {
        return tripRepository.findAllTripsByDate(date.atStartOfDay(), date.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional
    public void updateTripStatus(Integer tripId, String status, String note) {
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        trip.setStatus(status);
        if (note != null)
            trip.setStatusNote(note);
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public Trip createTrip(TripCreateRequestDTO request) {
        LocalDateTime departureTime = LocalDateTime.of(request.getDate(), request.getDepartureTime());
        if (departureTime.isBefore(LocalDateTime.now()))
            throw new BadRequestException("Thời gian khởi hành không được ở trong quá khứ!");

        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (driver.getUser() == null || driver.getUser().getRole() == null ||
                !"DRIVER".equalsIgnoreCase(driver.getUser().getRole().getRoleName())) {
            throw new BadRequestException("Người lái chính không phải là Tài xế (Role DRIVER)");
        }

        Driver subDriver = null;
        if (request.getSubDriverId() != null) {
            subDriver = driverRepository.findById(request.getSubDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sub-driver not found"));
            if (driver.getDriverId().equals(subDriver.getDriverId()))
                throw new BadRequestException("Trùng tài xế!");
            if (subDriver.getUser() == null || subDriver.getUser().getRole() == null ||
                    !"DRIVER".equalsIgnoreCase(subDriver.getUser().getRole().getRoleName())) {
                throw new BadRequestException("Phụ xe không phải là Tài xế (Role DRIVER)");
            }
        }

        Double durationHours = route.getEstimatedDuration() != null ? route.getEstimatedDuration() : 5.0;
        LocalDateTime arrivalTime = departureTime.plusMinutes((long) (durationHours * 60));

        if (tripRepository.existsByVehicleAndOverlap(request.getVehicleId(), departureTime, arrivalTime))
            throw new BadRequestException("Xe bận!");
        if (tripRepository.isPersonBusy(request.getDriverId(), departureTime, arrivalTime))
            throw new BadRequestException("Tài xế bận!");
        if (subDriver != null && tripRepository.isPersonBusy(subDriver.getDriverId(), departureTime, arrivalTime))
            throw new BadRequestException("Phụ xe bận!");

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSubDriver(subDriver);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setBasePrice(request.getPrice());
        trip.setStatus(TripStatus.WAITING.getDisplayName());
        trip.setOnlineBookingCutoff(60);
        trip.setIsFullyBooked(false);
        trip.setMinPassengers(1);
        trip.setAutoCancelIfNotEnough(false);

        Trip savedTrip = tripRepository.save(trip);
        generateSeatsForTrip(savedTrip, vehicle);
        return savedTrip;
    }

    private void generateSeatsForTrip(Trip trip, Vehicle vehicle) {
        if (vehicle.getVehicleType() == null)
            return;
        int totalSeats = vehicle.getVehicleType().getTotalSeats();
        List<TripSeat> seatsToSave = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            seatsToSave.add(TripSeat.builder()
                    .trip(trip)
                    .seatNumber("A" + String.format("%02d", i))
                    .seatType("Standard")
                    .status(SeatStatus.Available.getDisplayName())
                    .build());
        }
        tripSeatRepository.saveAll(seatsToSave);
    }

    @Override
    @Transactional
    public void deleteTrip(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if ("Running".equalsIgnoreCase(trip.getStatus()) || "Completed".equalsIgnoreCase(trip.getStatus())) {
            throw new BadRequestException("Cannot delete a trip that is Running or Completed.");
        }

        long activeTickets = ticketRepository.countActiveTicketsByTripId(tripId);

        if (activeTickets > 0) {
            throw new BadRequestException(
                    String.format(
                            "Cannot DELETE this trip because there are %d active tickets sold. Please CANCEL the trip instead to notify passengers.",
                            activeTickets));
        }

        tripRepository.delete(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsForDriver(Integer driverId, String status, Pageable pageable) {
        if (!driverRepository.existsById(driverId))
            throw new ResourceNotFoundException("Driver not found");
        return tripRepository.findTripsByDriverOrSubDriver(driverId, StringUtils.hasText(status) ? status : null,
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId) {
        if (!tripRepository.existsById(tripId))
            throw new ResourceNotFoundException("Trip not found");
        List<TripSeat> allSeats = tripSeatRepository.findAllSeatsByTripIdWithDetails(tripId);
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByTripIdWithDetails(tripId);
        List<Passenger> passengers = passengerRepository.findAllByTripIdWithDetails(tripId);

        Map<Integer, Ticket> seatToTicketMap = activeTickets.stream()
                .collect(Collectors.toMap(t -> t.getSeat().getSeatId(), t -> t, (e, r) -> e));
        Map<Integer, Passenger> ticketToPassengerMap = passengers.stream()
                .collect(Collectors.toMap(p -> p.getTicket().getTicketId(), p -> p, (e, r) -> e));

        List<PassengerOnTripResponseDTO> result = new ArrayList<>();
        for (TripSeat seat : allSeats) {
            Ticket ticket = seatToTicketMap.get(seat.getSeatId());
            if (ticket != null) {
                Passenger passenger = ticketToPassengerMap.get(ticket.getTicketId());
                result.add(passengerOnTripMapper.toDTO(seat, ticket, passenger));
            } else {
                result.add(passengerOnTripMapper.toSeatOnlyDTO(seat));
            }
        }
        return result;
    }

    @Override
    public TripDetailedResponseDTO enrichTripStats(TripDetailedResponseDTO dto, Integer tripId) {
        int booked = tripRepository.countBookedSeats(tripId);
        dto.setBookedSeats(booked);
        int checkedIn = tripRepository.countCheckedInSeats(tripId);
        dto.setCheckedInSeats(checkedIn);
        return dto;
    }

    @Override
    @Transactional
    public Trip updateTrip(Integer tripId, TripUpdateRequestDTO request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!"Waiting".equalsIgnoreCase(trip.getStatus()) && !"Delayed".equalsIgnoreCase(trip.getStatus())) {
            throw new BadRequestException("Only trips with status 'Waiting' or 'Delayed' can be edited.");
        }

        Vehicle newVehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        Driver newDriver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        long ticketsSold = ticketRepository.countActiveTicketsByTripId(tripId);
        int newCapacity = newVehicle.getVehicleType().getTotalSeats();

        if (newCapacity < ticketsSold) {
            throw new BadRequestException(
                    String.format("Cannot change vehicle. New capacity (%d) is less than tickets sold (%d).",
                            newCapacity, ticketsSold));
        }

        Driver subDriver = null;
        if (request.getSubDriverId() != null) {
            if (request.getDriverId().equals(request.getSubDriverId())) {
                throw new BadRequestException("Main driver and sub-driver cannot be the same person.");
            }
            subDriver = driverRepository.findById(request.getSubDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sub-driver not found"));
        }

        trip.setVehicle(newVehicle);
        trip.setDriver(newDriver);
        trip.setSubDriver(subDriver);
        trip.setBasePrice(request.getPrice());

        return tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponseDTO getTicketDetail(Integer ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        Trip trip = ticket.getBooking().getTrip();
        TripSeat seat = ticket.getSeat();
        Booking booking = ticket.getBooking();

        Passenger passenger = passengerRepository.findByTicketTicketId(ticketId)
                .orElse(null);

        return TicketDetailResponseDTO.builder()
                .ticketId(ticket.getTicketId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getTicketStatus())
                .passengerId(passenger != null ? passenger.getPassengerId() : null)
                .passengerName(passenger != null ? passenger.getFullName() : null)
                .phoneNumber(passenger != null ? passenger.getPhoneNumber() : null)
                .email(passenger != null ? passenger.getEmail() : null)
                .idCard(null)
                .tripId(trip.getTripId())
                .routeName(trip.getRoute().getOrigin().getLocationName() + " - " +
                        trip.getRoute().getDestination().getLocationName())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .originName(trip.getRoute().getOrigin().getLocationName())
                .destinationName(trip.getRoute().getDestination().getLocationName())
                .seatId(seat.getSeatId())
                .seatNumber(seat.getSeatNumber())
                .seatType(seat.getSeatType())
                .seatStatus(seat.getStatus())
                .bookingId(booking.getBookingId())
                .bookingDate(booking.getCreatedAt())
                .ticketPrice(ticket.getPrice().doubleValue())
                .paymentStatus(booking.getBookingStatus())
                .paymentMethod(booking.getBookingType())
                .vehiclePlateNumber(trip.getVehicle().getLicensePlate())
                .vehicleType(trip.getVehicle().getVehicleType().getTypeName())
                .build();
    }

    @Override
    @Transactional
    public void completeTrip(Integer tripId, CompleteTripRequestDTO request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        if (!trip.getDriver().getDriverId().equals(request.getDriverId()) &&
                (trip.getSubDriver() == null || !trip.getSubDriver().getDriverId().equals(request.getDriverId()))) {
            throw new BadRequestException("Only the assigned driver or sub-driver can complete this trip");
        }

        if (!TripStatus.RUNNING.getDisplayName().equalsIgnoreCase(trip.getStatus())) {
            throw new BadRequestException("Only running trips can be completed. Current status: " + trip.getStatus());
        }

        trip.setStatus(TripStatus.COMPLETED.getDisplayName());
        if (request.getCompletionNote() != null) {
            trip.setStatusNote(request.getCompletionNote());
        }
        trip.setArrivalTime(LocalDateTime.now());

        List<Ticket> confirmedTickets = ticketRepository.findByTripIdAndTicketStatus(
                tripId, TicketStatus.CONFIRMED.getDisplayName());

        for (Ticket ticket : confirmedTickets) {
            ticket.setTicketStatus(TicketStatus.NO_SHOW.getDisplayName());
        }

        if (!confirmedTickets.isEmpty()) {
            ticketRepository.saveAll(confirmedTickets);
        }

        tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getMyTripsForDriver(Integer userId, String status, Pageable pageable) {
        // Get driver by userId
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));

        // Reuse existing method with driverId
        return tripRepository.findTripsByDriverOrSubDriver(
                driver.getDriverId(),
                StringUtils.hasText(status) ? status : null,
                pageable);
    }

    public TripDetailedResponseDTO getTripDetailById(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        TripDetailedResponseDTO responseDTO = tripMapper.toDetailedDTO(trip);

        if (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null) {
            responseDTO.setTotalSeats(trip.getVehicle().getVehicleType().getTotalSeats());
        }

        return this.enrichTripStats(responseDTO, tripId);
    }
}