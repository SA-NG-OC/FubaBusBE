package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tripseats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seatid")
    private Integer seatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripid", nullable = false)
    private Trip trip;

    @Column(name = "seatnumber", nullable = false)
    private String seatNumber;

    @Column(name = "floornumber")
    private Integer floorNumber = 1;

    @Column(name = "seattype")
    private String seatType = "Thường";

    @Column(name = "status")
    private String status = "Trống";

    @Column(name = "holdexpiry")
    private LocalDateTime holdExpiry;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}