package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {

    @Query("""
        SELECT DISTINCT r
        FROM Route r
        LEFT JOIN r.origin o
        LEFT JOIN r.destination d
        LEFT JOIN r.routeStops rs
        LEFT JOIN rs.location l
        WHERE
            LOWER(r.routeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.locationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(d.locationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(l.locationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Route> searchRoutes(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
