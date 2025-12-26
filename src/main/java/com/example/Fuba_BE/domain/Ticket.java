package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticketid")
    private Integer ticketId;

    @Column(name = "ticketcode", nullable = false, unique = true)
    private String ticketCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seatid", nullable = false)
    private TripSeat seat;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "ticketstatus")
    private String ticketStatus = "Chưa xác nhận";

    @Column(name = "requirespassengerinfo")
    private Boolean requiresPassengerInfo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "printedby")
    private User printedBy;

    @Column(name = "printedat")
    private LocalDateTime printedAt;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}