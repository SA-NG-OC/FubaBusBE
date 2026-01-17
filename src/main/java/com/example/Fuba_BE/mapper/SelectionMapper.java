package com.example.Fuba_BE.mapper;

import org.springframework.stereotype.Component;

import com.example.Fuba_BE.domain.entity.Driver;
import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.domain.entity.Vehicle;
import com.example.Fuba_BE.dto.Driver.DriverSelectionDTO;
import com.example.Fuba_BE.dto.Routes.RouteSelectionDTO;
import com.example.Fuba_BE.dto.Vehicle.VehicleSelectionDTO;

@Component
public class SelectionMapper {

    public RouteSelectionDTO toRouteSelectionDTO(Route route) {
        if (route == null)
            return null;
        return new RouteSelectionDTO(route.getRouteId(), route.getRouteName());
    }

    public VehicleSelectionDTO toVehicleSelectionDTO(Vehicle vehicle) {
        if (vehicle == null)
            return null;
        String typeName = (vehicle.getVehicleType() != null) ? vehicle.getVehicleType().getTypeName() : "";
        return new VehicleSelectionDTO(
                vehicle.getVehicleId(),
                vehicle.getLicensePlate(),
                typeName);
    }

    public DriverSelectionDTO toDriverSelectionDTO(Driver driver) {
        if (driver == null)
            return null;
        // Giả định entity User có field fullName. Nếu không, bạn dùng
        // user.getUsername() hoặc chỉ dùng driverLicense.
        String driverName = (driver.getUser() != null) ? driver.getUser().getFullName() : "Unknown Driver";

        // Get avatar URL with default fallback
        String avatarUrl = "https://i.pinimg.com/736x/61/85/c3/6185c30215db7423445ee74c02e729b6.jpg";
        if (driver.getUser() != null && driver.getUser().getAvt() != null && !driver.getUser().getAvt().isEmpty()) {
            avatarUrl = driver.getUser().getAvt();
        }

        return new DriverSelectionDTO(
                driver.getDriverId(),
                driverName,
                driver.getDriverLicense(),
                avatarUrl);
    }
}