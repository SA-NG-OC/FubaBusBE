package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Vehicle.VehicleResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(source = "vehicleId", target = "vehicleid")
    @Mapping(source = "licensePlate", target = "licenseplate")
    @Mapping(source = "vehicleType.typeName", target = "vehicletype")
    @Mapping(source = "vehicleType.totalSeats", target = "totalseats")
    @Mapping(source = "status", target = "status")
    VehicleResponseDTO toResponseDTO(Vehicle vehicle);
}
