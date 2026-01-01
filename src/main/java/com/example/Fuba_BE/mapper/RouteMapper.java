package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RouteMapper {

    // 1. Map từ Entity sang Response DTO
    @Mapping(source = "origin.locationName", target = "originName")
    @Mapping(source = "destination.locationName", target = "destinationName")
    @Mapping(target = "estimatedTime", expression = "java(formatDuration(route.getEstimatedDuration()))")
    @Mapping(target = "totalStops", ignore = true) // Sẽ set trong service vì cần tính toán
    @Mapping(target = "stopNames", ignore = true)  // Sẽ set trong service
    RouteResponseDTO toResponseDTO(Route route);

    // 2. Map từ Request DTO sang Entity (Dùng cho Create)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "origin", ignore = true) // Sẽ set thủ công trong service vì cần query DB
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "status", constant = "Hoạt động")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Route toEntity(RouteRequestDTO request);

    // 3. Update Entity từ Request DTO (Dùng cho Edit)
    @Mapping(target = "routeId", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "status", ignore = true) // Giữ nguyên status cũ
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(RouteRequestDTO request, @MappingTarget Route route);

    // Helper: Format thời gian (Phút -> Chuỗi)
    default String formatDuration(Integer totalMinutes) {
        if (totalMinutes == null) return "0h";
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return minutes == 0 ? hours + "h" : hours + "h " + minutes + "m";
    }
}