package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passengerid")
    private Integer passengerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticketid", nullable = false, unique = true)
    private Ticket ticket;

    @Column(name = "fullname", nullable = false)
    private String fullName = "Khách";

    @Column(name = "phonenumber")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "dateofbirth")
    private LocalDate dateOfBirth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickuplocationid")
    private RouteStop pickupLocation;

    @Column(name = "pickupaddress")
    private String pickupAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropofflocationid")
    private RouteStop dropoffLocation;

    @Column(name = "dropoffaddress")
    private String dropoffAddress;

    @Column(name = "specialnote", columnDefinition = "TEXT")
    private String specialNote;

    @Column(name = "checkinstatus")
    private String checkinStatus = "Chưa lên xe";

    @Column(name = "checkintime")
    private LocalDateTime checkinTime;

    @Column(name = "checkinmethod", nullable = false)
    private String checkinMethod = "QR";

    @Column(name = "checkouttime")
    private LocalDateTime checkoutTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkedinby")
    private User checkedInBy;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

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