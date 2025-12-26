package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewid")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerid", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticketid", nullable = false)
    private Ticket ticket;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "driverrating")
    private Integer driverRating;

    @Column(name = "vehiclerating")
    private Integer vehicleRating;

    @Column(name = "servicerating")
    private Integer serviceRating;

    @Column(name = "punctualityrating")
    private Integer punctualityRating;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "reviewstatus")
    private String reviewStatus = "Đã duyệt";

    @Column(name = "adminresponse", columnDefinition = "TEXT")
    private String adminResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondedby")
    private User respondedBy;

    @Column(name = "respondedat")
    private LocalDateTime respondedAt;

    @Column(name = "reviewdate")
    private LocalDateTime reviewDate;

    @PrePersist
    protected void onCreate() {
        reviewDate = LocalDateTime.now();
    }
}