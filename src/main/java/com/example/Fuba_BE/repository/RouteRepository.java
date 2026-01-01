package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Integer> {
    // Có thể thêm method tìm kiếm hoặc lọc theo status nếu cần
    // List<Route> findByStatus(String status);
}