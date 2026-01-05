package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.RouteStop;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Integer> {

    List<RouteStop> findByRoute_RouteIdOrderByStopOrderAsc(Integer routeId);

    @Query("SELECT rs FROM RouteStop rs " +
            "LEFT JOIN FETCH rs.location " +
            "WHERE rs.route.routeId IN :routeIds " +
            "ORDER BY rs.route.routeId ASC, rs.stopOrder ASC")
    List<RouteStop> findByRouteIdIn(@Param("routeIds") List<Integer> routeIds);

    @Query("""
    SELECT new com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO(
        rs.stopId,
        rs.stopName
    )
    FROM RouteStop rs
    ORDER BY rs.stopOrder
    """)
    List<RouteStopResponseDTO> findAllRouteStopBasic();
}