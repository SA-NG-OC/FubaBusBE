package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Trip.TripCalendarDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.ResourceNotFoundException;
import com.example.Fuba_BE.mapper.TripMapper;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import com.example.Fuba_BE.repository.DriverRepository;
import com.example.Fuba_BE.repository.RouteRepository;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TripService implements ITripService {
    @Autowired
    private TripRepository tripRepository;
    private final TripMapper tripMapper;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }

    public Trip createTrip(Trip newTrip) {
        return tripRepository.save(newTrip);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        // Chuyển đổi LocalDate sang LocalDateTime để query
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        return tripRepository.findDistinctTripDates(start, end);
    }

    @Transactional(readOnly = true)
    public List<TripDetailedResponseDTO> getTripsDetailsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Trip> trips = tripRepository.findAllTripsByDate(startOfDay, endOfDay);

        // Dùng Mapper để chuyển đổi Entity -> DTO
        return trips.stream()
                .map(tripMapper::toDetailedDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TripDetailedResponseDTO> getTripsByStatus(String status, Pageable pageable) {

        Page<Trip> page;

        if (!StringUtils.hasText(status)) {
            // Không có status → lấy tất cả
            page = tripRepository.findAll(pageable);
        } else {
            page = tripRepository.findByStatus(status, pageable);
        }

        return page.map(tripMapper::toDetailedDTO);
    }

    @Transactional
    @Override
    public void updateTripStatus(Integer tripId, String status) {

        int updated = tripRepository.updateStatus(tripId, status);

        if (updated == 0) {
            throw new BadRequestException("Trip does not exist or status has not changed");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TripDetailedResponseDTO> getTripsByFilters(
            String status,
            LocalDate date,
            Pageable pageable
    ) {
        Page<Trip> page;

        boolean hasStatus = StringUtils.hasText(status);
        boolean hasDate = date != null;

        if (hasStatus && hasDate) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            page = tripRepository.findByStatusAndDepartureTimeBetween(
                    status, start, end, pageable
            );

        } else if (hasStatus) {
            page = tripRepository.findByStatus(status, pageable);

        } else if (hasDate) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            page = tripRepository.findByDepartureTimeBetween(
                    start, end, pageable
            );

        } else {
            page = tripRepository.findAll(pageable);
        }

        return page.map(tripMapper::toDetailedDTO);
    }

    @Override
    @Transactional
    public TripDetailedResponseDTO createTrip(TripCreateRequestDTO request) {
        // 1. Validate & Fetch Route
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found with id: " + request.getRouteId()));

        // 2. Validate & Fetch Vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + request.getVehicleId()));

        // 3. Validate & Fetch Driver
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + request.getDriverId()));

        // 4. Tính toán thời gian
        LocalDateTime departureDateTime = LocalDateTime.of(request.getDate(), request.getDepartureTime());

        // LƯU Ý: Tính thời gian đến (ArrivalTime).
        // Giả sử Entity Route có trường 'duration' (số giờ hoặc phút).
        // Nếu Route không có duration, bạn cần thêm vào hoặc tạm thời hard-code cộng giờ.
        // Ví dụ dưới đây giả định Route có getDuration() trả về double (số giờ) hoặc hardcode 5 tiếng.
        long durationInHours = 5; // Hoặc: (long) route.getDuration();
        LocalDateTime arrivalDateTime = departureDateTime.plusHours(durationInHours);

        // 5. Map dữ liệu vào Entity
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDepartureTime(departureDateTime);
        trip.setArrivalTime(arrivalDateTime);
        trip.setBasePrice(request.getPrice());

        // Set Default Values
        trip.setStatus("Waiting");
        trip.setOnlineBookingCutoff(60); // Mặc định 60 phút
        trip.setIsFullyBooked(false);
        trip.setMinPassengers(1);
        trip.setAutoCancelIfNotEnough(false);

        // Nếu muốn set người tạo (Lấy từ SecurityContext)
        // User currentUser = ... logic lấy user đang login ...
        // trip.setCreatedBy(currentUser);

        // 6. Lưu xuống DB
        Trip savedTrip = tripRepository.save(trip);

        // 7. Trả về DTO
        return tripMapper.toDetailedDTO(savedTrip);
    }

}
