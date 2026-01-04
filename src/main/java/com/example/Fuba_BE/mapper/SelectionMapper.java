package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.dto.Routes.RouteSelectionDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;
import org.springframework.stereotype.Component;

@Component
public class SelectionMapper {

    public RouteSelectionDTO toRouteSelectionDTO(Route route) {
        if (route == null) return null;
        return new RouteSelectionDTO(route.getRouteId(), route.getRouteName());
    }

    public VehicleSelectionDTO toVehicleSelectionDTO(Vehicle vehicle) {
        if (vehicle == null) return null;
        String typeName = (vehicle.getVehicleType() != null) ? vehicle.getVehicleType().getTypeName() : "";
        return new VehicleSelectionDTO(
                vehicle.getVehicleId(),
                vehicle.getLicensePlate(),
                typeName
        );
    }

    public DriverSelectionDTO toDriverSelectionDTO(Driver driver) {
        if (driver == null) return null;
        // Giả định entity User có field fullName. Nếu không, bạn dùng user.getUsername() hoặc chỉ dùng driverLicense.
        String driverName = (driver.getUser() != null) ? driver.getUser().getFullName() : "Unknown Driver";

        return new DriverSelectionDTO(
                driver.getDriverId(),
                driverName,
                driver.getDriverLicense()
        );
    }
}