package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

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

    @Column(name = "refundtype")
    private String refundType = "Hủy toàn bộ";

    @Column(name = "affectedticketids", columnDefinition = "TEXT")
    private String affectedTicketIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newtripid")
    private Trip newTrip;

    @Column(name = "pricedifference")
    private BigDecimal priceDifference = BigDecimal.ZERO;

    @Column(name = "refundstatus")
    private String refundStatus = "Đang xử lý";

    @Column(name = "refundmethod", nullable = false)
    private String refundMethod;

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