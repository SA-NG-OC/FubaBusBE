package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.Booking.BookingResponse;
import com.example.Fuba_BE.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper for converting Booking entities to DTOs.
 * Uses manual mapping due to complex nested structures.
 */
@Component
@RequiredArgsConstructor
public class BookingMapper {

    private final PassengerRepository passengerRepository;

    /**
     * Convert Booking entity to BookingResponse DTO
     */
    public BookingResponse toBookingResponse(Booking booking, Trip trip, List<Ticket> tickets) {
        return toBookingResponse(booking, trip, tickets, null);
    }

    /**
     * Convert Booking entity to BookingResponse DTO with pre-fetched passengers
     */
    public BookingResponse toBookingResponse(Booking booking, Trip trip, List<Ticket> tickets, 
                                            java.util.Map<Integer, Passenger> passengersByTicketId) {
        if (booking == null) {
            return null;
        }

        // Calculate remaining seconds if booking is Held or Pending
        Long remainingSeconds = null;
        if (booking.getHoldExpiry() != null && 
            ("Held".equals(booking.getBookingStatus()) || "Pending".equals(booking.getBookingStatus()))) {
            java.time.Duration duration = java.time.Duration.between(
                java.time.LocalDateTime.now(), 
                booking.getHoldExpiry()
            );
            remainingSeconds = duration.getSeconds();
            // If negative (expired), set to 0
            if (remainingSeconds < 0) {
                remainingSeconds = 0L;
            }
        }

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .bookingCode(booking.getBookingCode())
                .tripId(trip != null ? trip.getTripId() : null)
                .tripInfo(toTripInfo(trip))
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                .totalAmount(booking.getTotalAmount())
                .bookingStatus(booking.getBookingStatus())
                .bookingType(booking.getBookingType())
                .tickets(toTicketInfoList(tickets, passengersByTicketId))
                .createdAt(booking.getCreatedAt())
                .holdExpiry(booking.getHoldExpiry())
                .remainingSeconds(remainingSeconds)
                .build();
    }

    /**
     * Convert Trip entity to TripInfo DTO
     */
    private BookingResponse.TripInfo toTripInfo(Trip trip) {
        if (trip == null) {
            return null;
        }

        return BookingResponse.TripInfo.builder()
                .tripId(trip.getTripId())
                .routeName(getRouteName(trip))
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .pickupLocation(getPickupLocationName(trip))
                .pickupTime(trip.getDepartureTime())
                .dropoffLocation(getDropoffLocationName(trip))
                .dropoffTime(trip.getArrivalTime())
                .vehiclePlate(getVehiclePlate(trip))
                .driverName(getDriverName(trip))
                .build();
    }

    /**
     * Convert list of Ticket entities to TicketInfo DTOs
     */
    private List<BookingResponse.TicketInfo> toTicketInfoList(List<Ticket> tickets) {
        return toTicketInfoList(tickets, null);
    }

    /**
     * Convert list of Ticket entities to TicketInfo DTOs with pre-fetched passengers
     */
    private List<BookingResponse.TicketInfo> toTicketInfoList(List<Ticket> tickets, 
                                                              java.util.Map<Integer, Passenger> passengersByTicketId) {
        if (tickets == null || tickets.isEmpty()) {
            return new ArrayList<>();
        }

        List<BookingResponse.TicketInfo> ticketInfos = new ArrayList<>();
        for (Ticket ticket : tickets) {
            ticketInfos.add(toTicketInfo(ticket, passengersByTicketId));
        }
        return ticketInfos;
    }

    /**
     * Convert Ticket entity to TicketInfo DTO
     */
    private BookingResponse.TicketInfo toTicketInfo(Ticket ticket) {
        return toTicketInfo(ticket, null);
    }

    /**
     * Convert Ticket entity to TicketInfo DTO with pre-fetched passenger
     */
    private BookingResponse.TicketInfo toTicketInfo(Ticket ticket, 
                                                   java.util.Map<Integer, Passenger> passengersByTicketId) {
        if (ticket == null) {
            return null;
        }

        TripSeat seat = ticket.getSeat();
        
        // Get passenger from map if available, otherwise try database (for backward compatibility)
        BookingResponse.PassengerInfo passengerInfo = null;
        if (passengersByTicketId != null) {
            Passenger passenger = passengersByTicketId.get(ticket.getTicketId());
            if (passenger != null) {
                passengerInfo = toPassengerInfo(passenger);
            }
        } else {
            // Fallback to database query (only if map not provided)
            try {
                var passengerOpt = passengerRepository.findByTicket_TicketId(ticket.getTicketId());
                if (passengerOpt.isPresent()) {
                    passengerInfo = toPassengerInfo(passengerOpt.get());
                }
            } catch (Exception e) {
                // Passenger info is optional
            }
        }

        return BookingResponse.TicketInfo.builder()
                .ticketId(ticket.getTicketId())
                .ticketCode(ticket.getTicketCode())
                .seatId(seat != null ? seat.getSeatId() : null)
                .seatNumber(seat != null ? seat.getSeatNumber() : null)
                .floorNumber(seat != null ? seat.getFloorNumber() : null)
                .price(ticket.getPrice())
                .ticketStatus(ticket.getTicketStatus())
                .passenger(passengerInfo)
                .build();
    }

    /**
     * Convert Passenger entity to PassengerInfo DTO
     */
    private BookingResponse.PassengerInfo toPassengerInfo(Passenger passenger) {
        if (passenger == null) {
            return null;
        }

        return BookingResponse.PassengerInfo.builder()
                .passengerId(passenger.getPassengerId())
                .fullName(passenger.getFullName())
                .phoneNumber(passenger.getPhoneNumber())
                .email(passenger.getEmail())
                .pickupAddress(getPickupAddress(passenger))
                .dropoffAddress(getDropoffAddress(passenger))
                .build();
    }

    // ==================== Helper Methods ====================

    private String getRouteName(Trip trip) {
        if (trip.getRoute() == null) {
            return null;
        }
        return trip.getRoute().getRouteName();
    }

    private String getVehiclePlate(Trip trip) {
        if (trip.getVehicle() == null) {
            return null;
        }
        return trip.getVehicle().getLicensePlate();
    }

    private String getDriverName(Trip trip) {
        if (trip.getDriver() == null) {
            return null;
        }
        // Driver name is accessed through User relationship
        if (trip.getDriver().getUser() != null) {
            return trip.getDriver().getUser().getFullName();
        }
        return null;
    }

    private String getPickupLocationName(Trip trip) {
        if (trip.getRoute() == null || trip.getRoute().getOrigin() == null) {
            return null;
        }
        Location origin = trip.getRoute().getOrigin();
        // Return formatted: "LocationName, Province"
        if (origin.getProvince() != null) {
            return origin.getLocationName() + ", " + origin.getProvince();
        }
        return origin.getLocationName();
    }

    private String getDropoffLocationName(Trip trip) {
        if (trip.getRoute() == null || trip.getRoute().getDestination() == null) {
            return null;
        }
        Location destination = trip.getRoute().getDestination();
        // Return formatted: "LocationName, Province"
        if (destination.getProvince() != null) {
            return destination.getLocationName() + ", " + destination.getProvince();
        }
        return destination.getLocationName();
    }

    private String getPickupAddress(Passenger passenger) {
        if (passenger.getPickupAddress() != null) {
            return passenger.getPickupAddress();
        }
        if (passenger.getPickupLocation() != null) {
            return passenger.getPickupLocation().getStopName();
        }
        return null;
    }

    private String getDropoffAddress(Passenger passenger) {
        if (passenger.getDropoffAddress() != null) {
            return passenger.getDropoffAddress();
        }
        if (passenger.getDropoffLocation() != null) {
            return passenger.getDropoffLocation().getStopName();
        }
        return null;
    }
}
