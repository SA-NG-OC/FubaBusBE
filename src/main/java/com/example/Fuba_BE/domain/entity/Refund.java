package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    // Refund Type constants (DB constraint: FullCancellation, PartialCancellation, Reschedule)
    public static final String TYPE_FULL_CANCELLATION = "FullCancellation";
    public static final String TYPE_PARTIAL_CANCELLATION = "PartialCancellation";
    public static final String TYPE_RESCHEDULE = "Reschedule";
    
    // Refund Status constants (DB constraint: Pending, Refunded, Rejected)
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_REFUNDED = "Refunded";
    public static final String STATUS_REJECTED = "Rejected";
    
    // Refund Method constants (DB constraint: Transfer, Cash)
    public static final String METHOD_TRANSFER = "Transfer";
    public static final String METHOD_CASH = "Cash";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refundid")
    private Integer refundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @Column(name = "refundamount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "refundreason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refundtype", length = 30)
    @Builder.Default
    private String refundType = TYPE_FULL_CANCELLATION;

    @Column(name = "affectedticketids", columnDefinition = "TEXT")
    private String affectedTicketIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newtripid")
    private Trip newTrip;

    @Column(name = "pricedifference")
    @Builder.Default
    private BigDecimal priceDifference = BigDecimal.ZERO;

    @Column(name = "refundstatus", length = 30)
    @Builder.Default
    private String refundStatus = STATUS_PENDING;

    @Column(name = "refundmethod", nullable = false, length = 30)
    @Builder.Default
    private String refundMethod = METHOD_TRANSFER;

    @Column(name = "bankaccount")
    private String bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processedby")
    private User processedBy;

    @Column(name = "processedat")
    private LocalDateTime processedAt;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}