package com.example.Fuba_BE.dto.Ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketChangeResponseDTO {

    private Integer ticketId;
    private String ticketCode;
    private String status;

    // Old trip info
    private Integer oldTripId;
    private String oldRouteName;
    private LocalDateTime oldDepartureTime;
    private String oldSeatNumber;

    // New trip info
    private Integer newTripId;
    private String newRouteName;
    private LocalDateTime newDepartureTime;
    private String newSeatNumber;

    // Price difference (if any)
    private BigDecimal priceDifference;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;

    private String changeReason;
    private LocalDateTime changedAt;
}
