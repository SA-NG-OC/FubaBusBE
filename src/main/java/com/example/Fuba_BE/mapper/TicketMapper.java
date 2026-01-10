package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.Ticket.TicketScanResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    /**
     * Main mapping method.
     * Takes the Ticket (which contains Booking, Seat, Trip) and the Passenger entity.
     * Note: Passenger is passed separately because it's a 1-to-1 link usually fetched separately or joined.
     */
    @Mapping(target = "ticketInfo", expression = "java(mapTicketInfo(ticket))")
    @Mapping(target = "tripInfo", expression = "java(mapTripInfo(ticket.getBooking().getTrip()))")
    @Mapping(target = "passengerInfo", expression = "java(mapPassengerInfo(passenger, ticket.getBooking()))")
    @Mapping(target = "seatInfo", expression = "java(mapSeatInfo(ticket.getSeat()))")
    @Mapping(target = "pickupInfo", expression = "java(mapPickupInfo(ticket.getBooking().getTrip(), passenger))")
    @Mapping(target = "dropoffInfo", expression = "java(mapDropoffInfo(ticket.getBooking().getTrip(), passenger))")
    TicketScanResponseDTO toScanResponse(Ticket ticket, Passenger passenger);

    // --- Helper Methods (Internal Logic) ---

    default TicketScanResponseDTO.TicketInfoDTO mapTicketInfo(Ticket ticket) {
        if (ticket == null) return null;
        return TicketScanResponseDTO.TicketInfoDTO.builder()
                .ticketCode(ticket.getTicketCode())
                .bookingCode(ticket.getBooking() != null ? ticket.getBooking().getBookingCode() : null)
                .status(ticket.getTicketStatus())
                .price(ticket.getPrice())
                .build();
    }

    default TicketScanResponseDTO.TripInfoDTO mapTripInfo(Trip trip) {
        if (trip == null) return null;
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        // Calculate Duration
        Duration duration = Duration.between(trip.getDepartureTime(), trip.getArrivalTime());
        String durationStr = String.format("(%dh %02dm)", duration.toHours(), duration.toMinutesPart());

        // Time Range
        String timeRange = trip.getDepartureTime().format(timeFmt) + " - " + trip.getArrivalTime().format(timeFmt);

        // Vehicle & Driver
        String vType = (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null)
                ? trip.getVehicle().getVehicleType().getTypeName() : "";
        String plate = (trip.getVehicle() != null) ? trip.getVehicle().getLicensePlate() : "";
        String driver = (trip.getDriver() != null && trip.getDriver().getUser() != null)
                ? trip.getDriver().getUser().getFullName() : "Unassigned";

        // Route Name
        String routeName = trip.getRoute().getOrigin().getLocationName() + " \u2192 " + trip.getRoute().getDestination().getLocationName();

        return TicketScanResponseDTO.TripInfoDTO.builder()
                .routeName(routeName)
                .departureDate(trip.getDepartureTime().toLocalDate())
                .timeRange(timeRange)
                .duration(durationStr)
                .vehicleType(vType + " " + (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null ? trip.getVehicle().getVehicleType().getTotalSeats() + " seats" : ""))
                .licensePlate(plate)
                .driverName(driver)
                .build();
    }

    default TicketScanResponseDTO.PassengerInfoDTO mapPassengerInfo(Passenger passenger, Booking booking) {
        // Priority: Passenger Table > Booking Table
        String name = (passenger != null) ? passenger.getFullName() : booking.getCustomerName();
        String email = (passenger != null) ? passenger.getEmail() : booking.getCustomerEmail();
        String phone = (passenger != null) ? passenger.getPhoneNumber() : booking.getCustomerPhone();
        String cccd = "079123456789"; // Hardcoded logic or fetch from User table if linked

        return TicketScanResponseDTO.PassengerInfoDTO.builder()
                .fullName(name)
                .email(email)
                .phoneNumber(phone)
                .cccd(cccd)
                .build();
    }

    default TicketScanResponseDTO.SeatInfoDTO mapSeatInfo(TripSeat seat) {
        if (seat == null) return null;
        String floor = (seat.getFloorNumber() == 1) ? "Lower deck" : "Upper deck";
        // Simple logic for window/aisle, in reality, might need a specific field in DB
        String position = "Window";

        return TicketScanResponseDTO.SeatInfoDTO.builder()
                .seatNumber(seat.getSeatNumber())
                .floor(floor)
                .position(position)
                .build();
    }

    default TicketScanResponseDTO.LocationInfoDTO mapPickupInfo(Trip trip, Passenger passenger) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        String name, address, time;

        if (passenger != null && passenger.getPickupLocation() != null) {
            // Specific pickup point
            RouteStop stop = passenger.getPickupLocation();
            name = stop.getStopName();
            address = stop.getStopAddress(); // Or Province
            // Calculate pickup time: Trip Start + Estimated time from origin
            int offsetMinutes = (stop.getEstimatedTime() != null) ? stop.getEstimatedTime() : 0;
            time = trip.getDepartureTime().plusMinutes(offsetMinutes).format(timeFmt);
        } else {
            // Default to Route Origin
            name = trip.getRoute().getOrigin().getLocationName();
            address = trip.getRoute().getOrigin().getProvince();
            time = trip.getDepartureTime().format(timeFmt);
        }

        return TicketScanResponseDTO.LocationInfoDTO.builder()
                .locationName(name)
                .address(address)
                .time(time)
                .build();
    }

    default TicketScanResponseDTO.LocationInfoDTO mapDropoffInfo(Trip trip, Passenger passenger) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        String name, address, time;

        if (passenger != null && passenger.getDropoffLocation() != null) {
            // Specific dropoff point
            RouteStop stop = passenger.getDropoffLocation();
            name = stop.getStopName();
            address = stop.getStopAddress();
            // Estimate dropoff time (Approximation based on arrival or offset)
            int offsetMinutes = (stop.getEstimatedTime() != null) ? stop.getEstimatedTime() : 0;
            // If offset exists use it, else use arrival time
            time = (stop.getEstimatedTime() != null)
                    ? trip.getDepartureTime().plusMinutes(offsetMinutes).format(timeFmt)
                    : trip.getArrivalTime().format(timeFmt);
        } else {
            // Default to Route Destination
            name = trip.getRoute().getDestination().getLocationName();
            address = trip.getRoute().getDestination().getProvince();
            time = trip.getArrivalTime().format(timeFmt);
        }

        return TicketScanResponseDTO.LocationInfoDTO.builder()
                .locationName(name)
                .address(address)
                .time(time)
                .build();
    }
}