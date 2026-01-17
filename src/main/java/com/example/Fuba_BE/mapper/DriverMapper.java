package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.DriverRouteAssignment;
import com.example.Fuba_BE.dto.Driver.DriverResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    @Mapping(target = "userId", source = "driver.user.userId")
    @Mapping(target = "fullName", source = "driver.user.fullName")
    @Mapping(target = "email", source = "driver.user.email")
    @Mapping(target = "phoneNumber", source = "driver.user.phoneNumber")
    @Mapping(target = "avatar", source = "driver.user.avt")
    @Mapping(target = "status", source = "driver.user.status")
    @Mapping(target = "activeRoutes", source = "assignments", qualifiedByName = "mapActiveRoutes")
    DriverResponseDTO toResponseDTO(Driver driver, List<DriverRouteAssignment> assignments);

    @Named("mapActiveRoutes")
    default List<DriverResponseDTO.ActiveRouteDTO> mapActiveRoutes(List<DriverRouteAssignment> assignments) {
        if (assignments == null) {
            return List.of();
        }

        return assignments.stream()
                .map(assignment -> DriverResponseDTO.ActiveRouteDTO.builder()
                        .assignmentId(assignment.getAssignmentId())
                        .routeId(assignment.getRoute().getRouteId())
                        .routeName(assignment.getRoute().getRouteName())
                        .origin(assignment.getRoute().getOrigin().getLocationName())
                        .destination(assignment.getRoute().getDestination().getLocationName())
                        .preferredRole(assignment.getPreferredRole())
                        .priority(assignment.getPriority())
                        .startDate(assignment.getStartDate())
                        .endDate(assignment.getEndDate())
                        .build())
                .toList();
    }
}
