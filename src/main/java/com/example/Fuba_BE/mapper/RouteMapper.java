package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RouteMapper {

    // Entity -> Response DTO
    @Mapping(source = "origin.locationName", target = "originName")
    @Mapping(source = "destination.locationName", target = "destinationName")
    @Mapping(source = "estimatedDuration", target = "estimatedDuration")
    @Mapping(target = "totalStops", ignore = true)
    @Mapping(target = "stopNames", ignore = true)
    RouteResponseDTO toResponseDTO(Route route);

    // Request DTO -> Entity (Create)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "status", constant = "Active")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Route toEntity(RouteRequestDTO request);

    // Update Entity (Edit)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(RouteRequestDTO request, @MappingTarget Route route);
}
