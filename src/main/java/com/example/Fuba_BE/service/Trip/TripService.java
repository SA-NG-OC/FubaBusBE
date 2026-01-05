package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.TripMapper;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Dùng cái này thay vì Flyway util

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService implements ITripService {

    private final TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    // --- CÁC HÀM GET DỮ LIỆU ---

    @Override
    @Transactional(readOnly = true)
    public Page<TripDetailedResponseDTO> getTripsByFilters(String status, LocalDate date, Pageable pageable) {
        // Chuẩn bị dữ liệu filter
        String filterStatus = StringUtils.hasText(status) ? status : null;
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (date != null) {
            start = date.atStartOfDay();
            end = date.atTime(LocalTime.MAX);
        }

        Page<Trip> page = tripRepository.findTripsWithFilter(filterStatus, start, end, pageable);

        return page.map(tripMapper::toDetailedDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TripDetailedResponseDTO> getTripsByStatus(String status, Pageable pageable) {
        return getTripsByFilters(status, null, pageable);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        return tripRepository.findDistinctTripDates(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public List<TripDetailedResponseDTO> getTripsDetailsByDate(LocalDate date) {
        List<Trip> trips = tripRepository.findAllTripsByDate(
                date.atStartOfDay(), date.atTime(LocalTime.MAX)
        );
        return trips.stream().map(tripMapper::toDetailedDTO).collect(Collectors.toList());
    }

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    @Override
    @Transactional
    public TripDetailedResponseDTO createTrip(TripCreateRequestDTO request) {
        // 1. Fetch Related Entities (Dùng song song nếu muốn nhanh hơn, nhưng tuần tự cho an toàn)
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + request.getRouteId()));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));

        // 2. Logic Thời gian
        LocalDateTime departureTime = LocalDateTime.of(request.getDate(), request.getDepartureTime());
        long durationHours = 5;
        LocalDateTime arrivalTime = departureTime.plusHours(durationHours);

        // 3. Construct Entity
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setBasePrice(request.getPrice());

        // Default values
        trip.setStatus("Waiting");
        trip.setOnlineBookingCutoff(60);
        trip.setIsFullyBooked(false);
        trip.setMinPassengers(1);
        trip.setAutoCancelIfNotEnough(false);

        Trip savedTrip = tripRepository.save(trip);
        return tripMapper.toDetailedDTO(savedTrip);
    }

    @Override
    @Transactional
    public void updateTripStatus(Integer tripId, String status) {
        int updatedCount = tripRepository.updateStatus(tripId, status);
        if (updatedCount == 0) {
            throw new BadRequestException("Trip not found or status update failed");
        }
    }
}