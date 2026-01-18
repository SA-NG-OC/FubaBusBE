package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.dto.Location.CreateLocationRequestDTO;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationResponseDTO toResponseDTO(Location location);

    Location toEntity(CreateLocationRequestDTO request);
}