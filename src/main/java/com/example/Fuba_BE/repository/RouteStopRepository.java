package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Integer> {
    // Lấy các điểm dừng của một route, sắp xếp theo thứ tự
    List<RouteStop> findByRoute_RouteIdOrderByStopOrderAsc(Integer routeId);
}