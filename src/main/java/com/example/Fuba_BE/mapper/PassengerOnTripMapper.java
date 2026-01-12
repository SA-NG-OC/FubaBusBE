package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.entity.*;
import com.example.Fuba_BE.dto.Trip.PassengerOnTripResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PassengerOnTripMapper {

    /**
     * Map TripSeat to full PassengerOnTripResponseDTO.
     * Used when seat has booking/ticket/passenger info.
     */
    default PassengerOnTripResponseDTO toDTO(TripSeat seat, Ticket ticket, Passenger passenger) {
        return PassengerOnTripResponseDTO.builder()
                .seat(mapSeatInfo(seat))
                .ticket(mapTicketInfo(ticket))
                .passenger(mapPassengerInfo(passenger))
                .checkin(mapCheckinInfo(passenger))
                .build();
    }

    /**
     * Map TripSeat only (for available/empty seats).
     */
    default PassengerOnTripResponseDTO toSeatOnlyDTO(TripSeat seat) {
        return PassengerOnTripResponseDTO.builder()
                .seat(mapSeatInfo(seat))
                .ticket(null)
                .passenger(null)
                .checkin(null)
                .build();
    }

    default PassengerOnTripResponseDTO.SeatInfoDTO mapSeatInfo(TripSeat seat) {
        if (seat == null) return null;
        return PassengerOnTripResponseDTO.SeatInfoDTO.builder()
                .seatId(seat.getSeatId())
                .seatNumber(seat.getSeatNumber())
                .floorNumber(seat.getFloorNumber())
                .seatType(seat.getSeatType())
                .status(seat.getStatus())
                .build();
    }

    default PassengerOnTripResponseDTO.TicketInfoDTO mapTicketInfo(Ticket ticket) {
        if (ticket == null) return null;
        return PassengerOnTripResponseDTO.TicketInfoDTO.builder()
                .ticketId(ticket.getTicketId())
                .ticketCode(ticket.getTicketCode())
                .bookingCode(ticket.getBooking() != null ? ticket.getBooking().getBookingCode() : null)
                .ticketStatus(ticket.getTicketStatus())
                .price(ticket.getPrice())
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    default PassengerOnTripResponseDTO.PassengerInfoDTO mapPassengerInfo(Passenger passenger) {
        if (passenger == null) return null;
        return PassengerOnTripResponseDTO.PassengerInfoDTO.builder()
                .passengerId(passenger.getPassengerId())
                .fullName(passenger.getFullName())
                .phoneNumber(passenger.getPhoneNumber())
                .email(passenger.getEmail())
                .pickupLocationName(passenger.getPickupLocation() != null 
                        ? passenger.getPickupLocation().getStopName() : null)
                .pickupAddress(passenger.getPickupAddress())
                .dropoffLocationName(passenger.getDropoffLocation() != null 
                        ? passenger.getDropoffLocation().getStopName() : null)
                .dropoffAddress(passenger.getDropoffAddress())
                .specialNote(passenger.getSpecialNote())
                .build();
    }

    default PassengerOnTripResponseDTO.CheckinInfoDTO mapCheckinInfo(Passenger passenger) {
        if (passenger == null) return null;
        return PassengerOnTripResponseDTO.CheckinInfoDTO.builder()
                .checkinStatus(passenger.getCheckinStatus())
                .checkinTime(passenger.getCheckinTime())
                .checkoutTime(passenger.getCheckoutTime())
                .checkinMethod(passenger.getCheckinMethod())
                .checkedInByName(passenger.getCheckedInBy() != null 
                        ? passenger.getCheckedInBy().getFullName() : null)
                .build();
    }
}
