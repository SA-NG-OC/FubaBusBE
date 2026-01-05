package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Dashboard.DashboardTripDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context; // Dùng Context để truyền thêm tham số bên ngoài vào

import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    // Thêm tham số bookedCount vào hàm map
    @Mapping(target = "tripIdDisplay", expression = "java(formatTripId(trip.getTripId()))")
    @Mapping(target = "routeName", expression = "java(formatRouteName(trip))")
    @Mapping(source = "status", target = "status")
    @Mapping(target = "statusClass", expression = "java(mapStatusClass(trip.getStatus()))")
    @Mapping(target = "departure", expression = "java(mapToLocalTime(trip.getDepartureTime()))")
    // Sử dụng bookedCount được truyền vào thay vì tự tính
    @Mapping(target = "seatsInfo", expression = "java(formatSeats(trip, bookedCount))")
    DashboardTripDTO toDashboardTripDTO(Trip trip, @Context Long bookedCount);

    default LocalTime mapToLocalTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalTime() : null;
    }

    default String formatTripId(Integer id) {
        return id == null ? "TR-???" : String.format("TR-%03d", id);
    }

    default String formatRouteName(Trip trip) {
        if (trip.getRoute() == null) return "Unknown";
        String origin = (trip.getRoute().getOrigin() != null) ? trip.getRoute().getOrigin().getLocationName() : "?";
        String dest = (trip.getRoute().getDestination() != null) ? trip.getRoute().getDestination().getLocationName() : "?";
        return origin + " \u2192 " + dest;
    }

    default String mapStatusClass(String status) {
        if (status == null) return "status-default";
        switch (status.toLowerCase()) {
            case "running": return "status-running";
            case "waiting": return "status-waiting";
            case "finished": return "status-finished";
            case "cancelled": return "status-cancelled";
            default: return "status-default";
        }
    }

    // Logic format ghế đơn giản, không gọi DB nữa
    default String formatSeats(Trip trip, Long bookedCount) {
        int total = 0;
        if (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null) {
            total = trip.getVehicle().getVehicleType().getTotalSeats();
        }
        // bookedCount lấy từ SQL, đảm bảo chính xác và nhanh
        long booked = (bookedCount != null) ? bookedCount : 0;
        return booked + "/" + total;
    }
}