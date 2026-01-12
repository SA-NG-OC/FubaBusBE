package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Route;
import com.example.Fuba_BE.dto.AdminReport.RouteAnalyticsRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {

    @Query(value = """
        SELECT DISTINCT r
        FROM Route r
        LEFT JOIN FETCH r.origin o
        LEFT JOIN FETCH r.destination d
        LEFT JOIN r.routeStops rs
        LEFT JOIN rs.location l
        WHERE
            (:keyword IS NULL OR LOWER(r.routeName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(o.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(d.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(l.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
    """,
            countQuery = """
        SELECT count(DISTINCT r)
        FROM Route r
        LEFT JOIN r.origin o
        LEFT JOIN r.destination d
        LEFT JOIN r.routeStops rs
        LEFT JOIN rs.location l
        WHERE
            (:keyword IS NULL OR LOWER(r.routeName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(o.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(d.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            OR (:keyword IS NULL OR LOWER(l.locationName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
    """)
    Page<Route> searchRoutes(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Override
    @Query(value = "SELECT r FROM Route r LEFT JOIN FETCH r.origin LEFT JOIN FETCH r.destination",
            countQuery = "SELECT count(r) FROM Route r")
    Page<Route> findAll(Pageable pageable);

    List<Route> findByStatus(String status);

    @Query(value = """
           SELECT 
               r.routeid, 
               r.routename, 
               COALESCE(SUM(tc.revenue), 0) as totalRevenue, 
               COUNT(DISTINCT t.vehicleid) as vehicleCount, 
               COUNT(DISTINCT t.driverid) as driverCount 
           FROM routes r 
           LEFT JOIN trips t ON t.routeid = r.routeid AND t.departuretime BETWEEN :start AND :end 
           LEFT JOIN tripcosts tc ON tc.tripid = t.tripid 
           GROUP BY r.routeid, r.routename
           ORDER BY totalRevenue DESC 
           """,
            countQuery = "SELECT count(*) FROM routes",
            nativeQuery = true)
    Page<Object[]> findRoutesWithAnalytics(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end,
                                           Pageable pageable);
}