package com.example.Fuba_BE.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triptracking")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trackingid")
    private Integer trackingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false)
    private Trip trip;

    @Column(name = "currentlatitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "currentlongitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "currentaddress")
    private String currentAddress;

    @Column(name = "speed")
    private BigDecimal speed;

    @Column(name = "direction")
    private String direction;

    @Column(name = "estimatedarrival")
    private LocalDateTime estimatedArrival;

    @Column(name = "delayminutes")
    private Integer delayMinutes = 0;

    @Column(name = "delayreason", columnDefinition = "TEXT")
    private String delayReason;

    @Column(name = "trafficstatus")
    private String trafficStatus = "Bình thường";

    @Column(name = "recordedat")
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recordedby")
    private User recordedBy;

    @Column(name = "deviceinfo")
    private String deviceInfo;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}