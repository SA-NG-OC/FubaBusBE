package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.domain.enums.SeatStatus;
import com.example.Fuba_BE.domain.enums.TripStatus;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.PassengerOnTripMapper;
import com.example.Fuba_BE.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId, Pageable pageable) {
        String filterStatus = StringUtils.hasText(status) ? status : null;
        LocalDateTime start = null; LocalDateTime end = null;
        if (date != null) { start = date.atStartOfDay(); end = date.atTime(LocalTime.MAX); }
        return tripRepository.findTripsWithFilter(filterStatus, start, end, originId, destId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new BadRequestException("Start date cannot be after end date");
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
        if (note != null) trip.setStatusNote(note);
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public Trip createTrip(TripCreateRequestDTO request) {
        // Validate Time
        LocalDateTime departureTime = LocalDateTime.of(request.getDate(), request.getDepartureTime());
        if (departureTime.isBefore(LocalDateTime.now())) throw new BadRequestException("Thời gian khởi hành không được ở trong quá khứ!");

        // Fetch
        Route route = routeRepository.findById(request.getRouteId()).orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        Driver driver = driverRepository.findById(request.getDriverId()).orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        // Check Role Main Driver
        if (driver.getUser() == null || driver.getUser().getRole() == null ||
                !"DRIVER".equalsIgnoreCase(driver.getUser().getRole().getRoleName())) {
            throw new BadRequestException("Người lái chính không phải là Tài xế (Role DRIVER)");
        }

        // Sub Driver
        Driver subDriver = null;
        if (request.getSubDriverId() != null) {
            subDriver = driverRepository.findById(request.getSubDriverId()).orElseThrow(() -> new ResourceNotFoundException("Sub-driver not found"));
            if (driver.getDriverId().equals(subDriver.getDriverId())) throw new BadRequestException("Trùng tài xế!");
            if (subDriver.getUser() == null || subDriver.getUser().getRole() == null ||
                    !"DRIVER".equalsIgnoreCase(subDriver.getUser().getRole().getRoleName())) {
                throw new BadRequestException("Phụ xe không phải là Tài xế (Role DRIVER)");
            }
        }

        // Duration
        Double durationHours = route.getEstimatedDuration() != null ? route.getEstimatedDuration() : 5.0;
        LocalDateTime arrivalTime = departureTime.plusMinutes((long)(durationHours * 60));

        // Conflict Check
        if (tripRepository.existsByVehicleAndOverlap(request.getVehicleId(), departureTime, arrivalTime)) throw new BadRequestException("Xe bận!");
        if (tripRepository.isPersonBusy(request.getDriverId(), departureTime, arrivalTime)) throw new BadRequestException("Tài xế bận!");
        if (subDriver != null && tripRepository.isPersonBusy(subDriver.getDriverId(), departureTime, arrivalTime)) throw new BadRequestException("Phụ xe bận!");

        // Save
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSubDriver(subDriver);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        // trip.setDate(request.getDate()); <-- ĐÃ XÓA DÒNG GÂY LỖI NÀY
        trip.setBasePrice(request.getPrice()); // <-- Đã sửa lấy từ Request, không lấy từ Route nữa
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
        if (vehicle.getVehicleType() == null) return;
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
        Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new ResourceNotFoundException("Trip not found"));
        if (tripSeatRepository.existsByTrip_TripIdAndStatus(tripId, SeatStatus.Booked.getDisplayName())) {
            throw new BadRequestException("Không thể xóa chuyến đã có vé bán.");
        }
        tripSeatRepository.deleteByTrip_TripId(tripId);
        tripRepository.delete(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsForDriver(Integer driverId, String status, Pageable pageable) {
        if (!driverRepository.existsById(driverId)) throw new ResourceNotFoundException("Driver not found");
        return tripRepository.findTripsByDriverOrSubDriver(driverId, StringUtils.hasText(status) ? status : null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId) {
        if (!tripRepository.existsById(tripId)) throw new ResourceNotFoundException("Trip not found");
        List<TripSeat> allSeats = tripSeatRepository.findAllSeatsByTripIdWithDetails(tripId);
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByTripIdWithDetails(tripId);
        List<Passenger> passengers = passengerRepository.findAllByTripIdWithDetails(tripId);

        Map<Integer, Ticket> seatToTicketMap = activeTickets.stream().collect(Collectors.toMap(t -> t.getSeat().getSeatId(), t -> t, (e, r) -> e));
        Map<Integer, Passenger> ticketToPassengerMap = passengers.stream().collect(Collectors.toMap(p -> p.getTicket().getTicketId(), p -> p, (e, r) -> e));

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

    // --- HÀM NÀY GIÚP HIỂN THỊ SỐ LIỆU ---
    @Override
    public TripDetailedResponseDTO enrichTripStats(TripDetailedResponseDTO dto, Integer tripId) {
        int booked = tripRepository.countBookedSeats(tripId);
        dto.setBookedSeats(booked);
        int checkedIn = tripRepository.countCheckedInSeats(tripId);
        dto.setCheckedInSeats(checkedIn);
        return dto;
    }
}