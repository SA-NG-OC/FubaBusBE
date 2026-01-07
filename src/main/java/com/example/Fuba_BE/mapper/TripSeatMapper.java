package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.TripSeat;
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
        if (status == null) return "Available";
        return switch (status) {
            case "Available" -> "Available";
            case "Held" -> "SELECTED";
            case "Booked" -> "Booked";
            default -> "Available";
        };
    }
}
