package com.example.Fuba_BE.service.Trip;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.domain.enums.SeatStatus;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripCreateRequestDTO;
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
import com.example.Fuba_BE.domain.enums.TripStatus;
import com.example.Fuba_BE.domain.enums.SeatStatus;

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

    // --- CÁC HÀM GET DỮ LIỆU ---

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsByFilters(String status, LocalDate date, Integer originId, Integer destId, Pageable pageable) {
        // Xử lý status rỗng
        String filterStatus = StringUtils.hasText(status) ? status : null;

        // Xử lý ngày giờ (Tìm trong khoảng từ 00:00 đến 23:59 của ngày đó)
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (date != null) {
            start = date.atStartOfDay();
            end = date.atTime(LocalTime.MAX);
        }

        // Gọi Repository với các tham số mới
        return tripRepository.findTripsWithFilter(filterStatus, start, end, originId, destId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> getDaysWithTrips(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        return tripRepository.findDistinctTripDates(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> getTripsDetailsByDate(LocalDate date) {
        return tripRepository.findAllTripsByDate(
                date.atStartOfDay(), date.atTime(LocalTime.MAX)
        );
    }

    @Override
    @Transactional
    public void updateTripStatus(Integer tripId, String status, String note) {
        // 1. Tìm chuyến xe (để check tồn tại và lấy entity)
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // 2. Validate trạng thái hợp lệ (Chỉ chấp nhận các status trong Enum)
        try {
            // Kiểm tra xem status gửi lên có đúng chính tả trong Enum không
            // (Lưu ý: FE gửi lên chuỗi hiển thị 'Running', ta cần map ngược lại hoặc dùng chuẩn UPPERCASE tùy convention của bạn)
            // Ở đây mình giả định FE gửi đúng chuỗi khớp với DB (Ví dụ: "Running", "Delayed")
            boolean isValid = false;
            for (TripStatus ts : TripStatus.values()) {
                if (ts.getDisplayName().equals(status)) {
                    isValid = true;
                    break;
                }
            }
            if (!isValid) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }

        // 3. Cập nhật
        trip.setStatus(status);

        // Nếu có ghi chú (ví dụ lý do hoãn), cập nhật luôn
        if (note != null) {
            trip.setStatusNote(note);
        }

        // 4. Lưu lại
        tripRepository.save(trip);
    }

    // --- HÀM TẠO CHUYẾN XE (Create Trip) ---
    @Override
    @Transactional
    public Trip createTrip(TripCreateRequestDTO request) {
        // 1. Fetch Entities cơ bản
        Route route = routeRepository.findById(request.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + request.getRouteId()));
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + request.getVehicleId()));
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found: " + request.getDriverId()));

        // 2. Xử lý Phụ xe (Sub Driver) - Có thể null
        Driver subDriver = null;
        if (request.getSubDriverId() != null) {
            subDriver = driverRepository.findById(request.getSubDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sub-driver not found: " + request.getSubDriverId()));

            // Validate: Tài xế chính và Phụ xe không được là một người
            if (driver.getDriverId().equals(subDriver.getDriverId())) {
                throw new BadRequestException("Tài xế chính và Phụ xe không được trùng nhau!");
            }
        }

        // 3. Logic Thời gian
        LocalDateTime departureTime = LocalDateTime.of(request.getDate(), request.getDepartureTime());
        int durationMinutes = route.getEstimatedDuration() != null ? route.getEstimatedDuration() : 300;
        LocalDateTime arrivalTime = departureTime.plusMinutes(durationMinutes);

        if (arrivalTime.isBefore(departureTime)) {
            throw new BadRequestException("Arrival time cannot be before departure time");
        }

        // --- 4. KIỂM TRA XUNG ĐỘT (CONFLICT CHECK) ---

        // A. Check trùng Xe
        if (tripRepository.existsByVehicleAndOverlap(request.getVehicleId(), departureTime, arrivalTime)) {
            throw new BadRequestException("Chiếc xe này đang bận chạy chuyến khác trong khung giờ này!");
        }

        // B. Check trùng Tài xế chính
        if (tripRepository.isPersonBusy(request.getDriverId(), departureTime, arrivalTime)) {
            throw new BadRequestException("Tài xế chính đang bận (lái hoặc phụ) chuyến khác trong khung giờ này!");
        }

        // C. Check trùng Phụ xe (nếu có chọn phụ xe)
        if (subDriver != null) {
            if (tripRepository.isPersonBusy(subDriver.getDriverId(), departureTime, arrivalTime)) {
                throw new BadRequestException("Phụ xe đang bận (lái hoặc phụ) chuyến khác trong khung giờ này!");
            }
        }
        // ---------------------------------------------

        // 5. Construct Trip Entity
        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setSubDriver(subDriver); // Set phụ xe
        trip.setDepartureTime(departureTime);
        trip.setArrivalTime(arrivalTime);
        trip.setBasePrice(request.getPrice());

        // Status tiếng Anh chuẩn
        trip.setStatus(com.example.Fuba_BE.domain.enums.TripStatus.WAITING.getDisplayName());

        trip.setOnlineBookingCutoff(60);
        trip.setIsFullyBooked(false);
        trip.setMinPassengers(1);
        trip.setAutoCancelIfNotEnough(false);

        Trip savedTrip = tripRepository.save(trip);

        // 6. Generate Seats
        generateSeatsForTrip(savedTrip, vehicle);

        return savedTrip;
    }

    // --- LOGIC SINH GHẾ (ENGLISH VERSION) ---
    private void generateSeatsForTrip(Trip trip, Vehicle vehicle) {
        if (vehicle.getVehicleType() == null) {
            return;
        }

        int totalSeats = vehicle.getVehicleType().getTotalSeats();
        int floors = vehicle.getVehicleType().getNumberOfFloors() != null ? vehicle.getVehicleType().getNumberOfFloors() : 1;

        List<TripSeat> seatsToSave = new ArrayList<>();

        if (floors <= 1) {
            // 1 Floor: A01...A{N}
            seatsToSave.addAll(createFloorSeats(trip, 1, "A", totalSeats));
        } else if (floors == 2) {
            // 2 Floors: Floor 1 (A), Floor 2 (B)
            int perFloor = totalSeats / 2;
            seatsToSave.addAll(createFloorSeats(trip, 1, "A", perFloor));
            seatsToSave.addAll(createFloorSeats(trip, 2, "B", totalSeats - perFloor));
        }

        tripSeatRepository.saveAll(seatsToSave);
    }

    private List<TripSeat> createFloorSeats(Trip trip, int floorNumber, String prefix, int count) {
        List<TripSeat> list = new ArrayList<>();

        // 1. Status: Lấy từ Enum đã sửa ("Available")
        final String STATUS_TO_SAVE = SeatStatus.Available.getDisplayName();

        final String TYPE_NORMAL = "Standard";

        for (int i = 1; i <= count; i++) {
            TripSeat seat = TripSeat.builder()
                    .trip(trip)
                    .floorNumber(floorNumber)
                    .seatNumber(prefix + String.format("%02d", i))
                    .seatType(TYPE_NORMAL)
                    .status(STATUS_TO_SAVE)
                    .build();
            list.add(seat);
        }
        return list;
    }

    @Override
    @Transactional
    public void deleteTrip(Integer tripId) {
        // 1. Kiểm tra chuyến xe có tồn tại không
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found with id: " + tripId));

        // 2. Kiểm tra xem có vé nào đã bán (Booked) chưa?
        // Nếu có ghế "Booked" -> Chặn không cho xóa
        boolean hasBookings = tripSeatRepository.existsByTrip_TripIdAndStatus(
                tripId,
                SeatStatus.Booked.getDisplayName() // "Booked"
        );

        if (hasBookings) {
            throw new BadRequestException("Không thể xóa chuyến xe đã có vé bán ra. Hãy dùng chức năng Hủy chuyến (Cancel).");
        }

        // 3. Nếu an toàn (chưa ai mua), xóa sạch ghế trước
        tripSeatRepository.deleteByTrip_TripId(tripId);

        // 4. Cuối cùng xóa chuyến xe
        tripRepository.delete(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Trip> getTripsForDriver(Integer driverId, String status, Pageable pageable) {
        // 1. Kiểm tra tài xế có tồn tại không
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Driver not found with id: " + driverId);
        }

        // 2. Xử lý status rỗng
        String filterStatus = StringUtils.hasText(status) ? status : null;

        // 3. Gọi Repository (Tìm cả vai chính lẫn vai phụ)
        return tripRepository.findTripsByDriverOrSubDriver(driverId, filterStatus, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassengerOnTripResponseDTO> getPassengersOnTrip(Integer tripId) {
        // 1. Kiểm tra chuyến xe có tồn tại không
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException("Trip not found with id: " + tripId);
        }

        // 2. Lấy tất cả ghế của chuyến xe
        List<TripSeat> allSeats = tripSeatRepository.findAllSeatsByTripIdWithDetails(tripId);

        // 3. Lấy tất cả vé active của chuyến xe (không bao gồm Cancelled)
        List<Ticket> activeTickets = ticketRepository.findActiveTicketsByTripIdWithDetails(tripId);

        // 4. Lấy tất cả hành khách có vé active
        List<Passenger> passengers = passengerRepository.findAllByTripIdWithDetails(tripId);

        // 5. Tạo Map để lookup nhanh: seatId -> Ticket
        Map<Integer, Ticket> seatToTicketMap = activeTickets.stream()
                .collect(Collectors.toMap(
                        ticket -> ticket.getSeat().getSeatId(),
                        ticket -> ticket,
                        (existing, replacement) -> existing // Giữ cái đầu tiên nếu trùng
                ));

        // 6. Tạo Map để lookup nhanh: ticketId -> Passenger
        Map<Integer, Passenger> ticketToPassengerMap = passengers.stream()
                .collect(Collectors.toMap(
                        passenger -> passenger.getTicket().getTicketId(),
                        passenger -> passenger,
                        (existing, replacement) -> existing
                ));

        // 7. Map tất cả ghế sang DTO
        List<PassengerOnTripResponseDTO> result = new ArrayList<>();
        for (TripSeat seat : allSeats) {
            Ticket ticket = seatToTicketMap.get(seat.getSeatId());
            Passenger passenger = null;

            if (ticket != null) {
                passenger = ticketToPassengerMap.get(ticket.getTicketId());
                result.add(passengerOnTripMapper.toDTO(seat, ticket, passenger));
            } else {
                // Ghế trống (Available hoặc Held)
                result.add(passengerOnTripMapper.toSeatOnlyDTO(seat));
            }
        }

        return result;
    }
}