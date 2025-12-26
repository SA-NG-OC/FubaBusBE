package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paymentid")
    private Integer paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingid", nullable = false)
    private Booking booking;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "paymentmethod", nullable = false)
    private String paymentMethod;

    @Column(name = "paymentstatus")
    private String paymentStatus = "Chờ xử lý";

    @Column(name = "transactionid")
    private String transactionId;

    @Column(name = "paymentgateway")
    private String paymentGateway;

    @Column(name = "paymentnote", columnDefinition = "TEXT")
    private String paymentNote;

    @Column(name = "paidat")
    private LocalDateTime paidAt;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}