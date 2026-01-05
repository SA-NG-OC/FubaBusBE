package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tripid")
    private Integer tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routeid", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicleid", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driverid", nullable = false)
    private Driver driver;

    @Column(name = "departuretime", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrivaltime", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "baseprice", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "status")
    private String status = "Waiting";

    @Column(name = "statusnote", columnDefinition = "TEXT")
    private String statusNote;

    @Column(name = "onlinebookingcutoff")
    private Integer onlineBookingCutoff = 60;

    @Column(name = "isfullybooked")
    private Boolean isFullyBooked = false;

    @Column(name = "minpassengers")
    private Integer minPassengers = 1;

    @Column(name = "autocancelifnotenough")
    private Boolean autoCancelIfNotEnough = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby")
    private User createdBy;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "trip", fetch = FetchType.LAZY)
    private List<TripSeat> tripSeats;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}