package com.example.Fuba_BE.service;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.domain.entity.TripSeat;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.domain.entity.VehicleType;
import com.example.Fuba_BE.dto.seat.MigrateSeatMapRequest;
import com.example.Fuba_BE.dto.seat.SeatMapResponse;
import com.example.Fuba_BE.dto.seat.TripSeatDto;
import com.example.Fuba_BE.exception.BadRequestException;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.mapper.TripSeatMapper;
import com.example.Fuba_BE.repository.TripRepository;
import com.example.Fuba_BE.repository.TripSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatMapServiceImpl implements SeatMapService {

    // status chuẩn để FE dễ dùng (khớp domain string hiện tại)
    private static final String STATUS_AVAILABLE = "Trống";
    private static final String STATUS_LOCKED = "Đang giữ";
    private static final String STATUS_BOOKED = "Đã đặt";

    private final TripRepository tripRepository;
    private final TripSeatRepository tripSeatRepository;
    private final TripSeatMapper tripSeatMapper;

    public SeatMapServiceImpl(
            TripRepository tripRepository,
            TripSeatRepository tripSeatRepository,
            TripSeatMapper tripSeatMapper
    ) {
        this.tripRepository = tripRepository;
        this.tripSeatRepository = tripSeatRepository;
        this.tripSeatMapper = tripSeatMapper;
    }

    @Override
    @Transactional
    public SeatMapResponse migrateSeatMap(Integer tripId, MigrateSeatMapRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));

        // ⚠️ đảm bảo Trip có getVehicle()
        Vehicle vehicle = trip.getVehicle();
        if (vehicle == null) {
            throw new BadRequestException("Trip " + tripId + " has no vehicle assigned.");
        }

        VehicleType type = vehicle.getVehicleType();
        if (type == null) {
            throw new BadRequestException("Vehicle " + vehicle.getVehicleId() + " has no vehicleType assigned.");
        }

        boolean existed = tripSeatRepository.existsByTrip_TripId(tripId);
        if (existed && !request.isOverwrite()) {
            throw new BadRequestException("Seat map already exists for trip " + tripId + ". Use overwrite=true to recreate.");
        }

        if (existed && request.isOverwrite()) {
            tripSeatRepository.deleteByTrip_TripId(tripId);
        }

        // ===== Generate seat_map theo Figma =====
        int floors = Optional.ofNullable(type.getNumberOfFloors()).orElse(1);
        int totalSeats = Optional.ofNullable(type.getTotalSeats()).orElse(0);

        if (totalSeats <= 0) {
            throw new BadRequestException("VehicleType totalSeats must be > 0.");
        }

        List<TripSeat> generated = generateSeats(trip, floors, totalSeats);
        tripSeatRepository.saveAll(generated);

        return buildSeatMapResponse(tripId, vehicle, type, generated);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Integer tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new NotFoundException("Trip not found: " + tripId));

        Vehicle vehicle = trip.getVehicle();
        if (vehicle == null) {
            throw new BadRequestException("Trip " + tripId + " has no vehicle assigned.");
        }

        VehicleType type = vehicle.getVehicleType();
        if (type == null) {
            throw new BadRequestException("Vehicle " + vehicle.getVehicleId() + " has no vehicleType assigned.");
        }

        List<TripSeat> seats = tripSeatRepository.findByTrip_TripIdOrderByFloorNumberAscSeatNumberAsc(tripId);
        if (seats.isEmpty()) {
            throw new NotFoundException("Seat map not found for trip " + tripId + ". Please migrate seat map first.");
        }

        return buildSeatMapResponse(tripId, vehicle, type, seats);
    }

    /**
     * Generate seat labels theo UI:
     * - 1 tầng: A1..A{N}
     * - 2 tầng: chia đều (floor1: A1..A{n/2}, floor2: B1..B{n/2})
     */
    private List<TripSeat> generateSeats(Trip trip, int floors, int totalSeats) {
        if (floors <= 1) {
            return createFloorSeats(trip, 1, "A", totalSeats);
        }

        if (floors == 2) {
            if (totalSeats % 2 != 0) {
                throw new BadRequestException("For 2 floors, totalSeats should be even. Current=" + totalSeats);
            }
            int perFloor = totalSeats / 2;
            List<TripSeat> all = new ArrayList<>();
            all.addAll(createFloorSeats(trip, 1, "A", perFloor)); // Lower Floor: A1..A20
            all.addAll(createFloorSeats(trip, 2, "B", perFloor)); // Upper Floor: B1..B20
            return all;
        }

        // nếu sau này có 3 tầng thì xử lý thêm, còn hiện tại chặn
        throw new BadRequestException("Unsupported numberOfFloors: " + floors);
    }

    private List<TripSeat> createFloorSeats(Trip trip, int floorNumber, String prefix, int count) {
        List<TripSeat> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            TripSeat s = new TripSeat();
            s.setTrip(trip);
            s.setFloorNumber(floorNumber);
            s.setSeatNumber(prefix + i);     // A1..A20 / B1..B20
            s.setSeatType("Thường");
            s.setStatus(STATUS_AVAILABLE);
            s.setHoldExpiry(null);
            list.add(s);
        }
        return list;
    }

    private SeatMapResponse buildSeatMapResponse(Integer tripId, Vehicle vehicle, VehicleType type, List<TripSeat> seats) {
        Map<Integer, List<TripSeatDto>> byFloor = seats.stream()
                .collect(Collectors.groupingBy(
                        s -> Optional.ofNullable(s.getFloorNumber()).orElse(1),
                        TreeMap::new,
                        Collectors.mapping(tripSeatMapper::toDto, Collectors.toList())
                ));

        List<SeatMapResponse.FloorSeats> floors = byFloor.entrySet().stream()
                .map(e -> new SeatMapResponse.FloorSeats(
                        e.getKey(),
                        e.getKey() == 1 ? "Lower Floor" : "Upper Floor",
                        e.getValue()
                ))
                .toList();

        return new SeatMapResponse(
                tripId,
                vehicle.getVehicleId(),
                type.getTypeName(),
                Optional.ofNullable(type.getNumberOfFloors()).orElse(1),
                floors
        );
    }
}
