package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookingid")
    private Integer bookingId;

    @Column(name = "bookingcode", nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerid")
    private User customer;

    @Column(name = "customername", nullable = false)
    private String customerName;

    @Column(name = "customerphone", nullable = false)
    private String customerPhone;

    @Column(name = "customeremail")
    private String customerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false)
    private Trip trip;

    @Column(name = "totalamount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "bookingstatus")
    @Builder.Default
    private String bookingStatus = "Held";

    @Column(name = "bookingtype")
    @Builder.Default
    private String bookingType = "Online";

    @Column(name = "isguestbooking")
    @Builder.Default
    private Boolean isGuestBooking = false;

    @Column(name = "guestsessionid")
    private String guestSessionId;

    @Column(name = "invitationsentat")
    private LocalDateTime invitationSentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby")
    private User createdBy;

    @Column(name = "createdat", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @Column(name = "holdexpiry", nullable = false)
    private LocalDateTime holdExpiry;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        // Set default hold expiry to 15 minutes from creation
        // This will be overridden by booking service if needed
        if (holdExpiry == null) {
            holdExpiry = now.plusMinutes(15);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // CRITICAL: Never update createdAt or holdExpiry in @PreUpdate
        // holdExpiry should only be set explicitly by business logic
    }
}