package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "routestops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stopid")
    private Integer stopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routeid", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locationid", nullable = false)
    private Location location;

    @Column(name = "stoporder", nullable = false)
    private Integer stopOrder;

    @Column(name = "stoptype", nullable = false)
    private String stopType;

    @Column(name = "stopname")
    private String stopName;

    @Column(name = "ispickuppoint")
    private Boolean isPickupPoint = true;

    @Column(name = "isdropoffpoint")
    private Boolean isDropoffPoint = true;

    @Column(name = "stopaddress", columnDefinition = "TEXT")
    private String stopAddress;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "distancefromorigin")
    private BigDecimal distanceFromOrigin;

    @Column(name = "estimatedtime")
    private Integer estimatedTime;

    @Column(name = "stopnote", columnDefinition = "TEXT")
    private String stopNote;
}