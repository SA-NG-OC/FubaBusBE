package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Trip;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TripMapper {

    // Map từ Entity sang Response DTO chi tiết cho bảng
    @Mapping(source = "tripId", target = "tripId")
    @Mapping(target = "routeName", expression = "java(formatRouteName(trip))")
    @Mapping(target = "vehicleInfo", expression = "java(formatVehicleInfo(trip))")
    @Mapping(source = "driver.user.fullName", target = "driverName")
    @Mapping(source = "subDriver.user.fullName", target = "subDriverName")
    @Mapping(target = "date", expression = "java(trip.getDepartureTime().toLocalDate())")
    @Mapping(target = "departureTime", expression = "java(trip.getDepartureTime().toLocalTime())")
    @Mapping(target = "arrivalTime", expression = "java(trip.getArrivalTime().toLocalTime())")
    @Mapping(source = "basePrice", target = "price")
    @Mapping(source = "status", target = "status")
    TripDetailedResponseDTO toDetailedDTO(Trip trip);

    // Helper: Format tên tuyến (Origin -> Destination)
    default String formatRouteName(Trip trip) {
        if (trip.getRoute() == null) return "Unknown Route";
        String origin = trip.getRoute().getOrigin() != null ? trip.getRoute().getOrigin().getLocationName() : "?";
        String dest = trip.getRoute().getDestination() != null ? trip.getRoute().getDestination().getLocationName() : "?";
        return origin + " -> " + dest;
    }

    // Helper: Format thông tin xe (Biển số + Loại xe)
    default String formatVehicleInfo(Trip trip) {
        if (trip.getVehicle() == null) return "Unknown Vehicle";
        String plate = trip.getVehicle().getLicensePlate();
        String type = (trip.getVehicle().getVehicleType() != null) ? trip.getVehicle().getVehicleType().getTypeName() : "";
        // Kết quả VD: 51A-12345 (Giường nằm)
        return plate + " (" + type + ")";
    }
}