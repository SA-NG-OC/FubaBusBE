package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.TripSeat;
import com.example.Fuba_BE.dto.seat.TripSeatDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TripSeatMapper {

    @Mapping(source = "status", target = "status", qualifiedByName = "mapSeatStatus")
    TripSeatDto toDto(TripSeat seat);

    @Named("mapSeatStatus")
    default String mapSeatStatus(String status) {
        if (status == null) return "AVAILABLE";
        return switch (status) {
            case "Trống" -> "AVAILABLE";
            case "Đang giữ" -> "SELECTED";
            case "Đã đặt" -> "BOOKED";
            default -> "AVAILABLE";
        };
    }
}
