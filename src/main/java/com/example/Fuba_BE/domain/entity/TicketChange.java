package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticketchanges")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "changeid")
    private Integer changeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticketid", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oldtripid", nullable = false)
    private Trip oldTrip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newtripid", nullable = false)
    private Trip newTrip;

    @Column(name = "oldseatid", nullable = false)
    private Integer oldSeatId;

    @Column(name = "newseatid", nullable = false)
    private Integer newSeatId;

    @Column(name = "oldprice")
    private BigDecimal oldPrice;

    @Column(name = "newprice")
    private BigDecimal newPrice;

    @Column(name = "pricedifference")
    private BigDecimal priceDifference;

    @Column(name = "changereason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "changefee")
    private BigDecimal changeFee = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changedby")
    private User changedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approvedby")
    private User approvedBy;

    @Column(name = "changestatus")
    private String changeStatus = "Pending";

    @Column(name = "changedate")
    private LocalDateTime changeDate;

    @PrePersist
    protected void onCreate() {
        changeDate = LocalDateTime.now();
    }
}