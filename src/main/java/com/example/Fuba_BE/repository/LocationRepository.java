package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {

    // 1. Tìm kiếm địa điểm theo tên (Hỗ trợ Search box trong Dropdown)
    // Ví dụ: Nhập "Ho" sẽ ra "Ho Chi Minh", "Hoi An"...
    List<Location> findByLocationNameContainingIgnoreCase(String keyword);

    // 2. Lấy danh sách tất cả địa điểm và sắp xếp theo tên A-Z (Dùng để load list ban đầu)
    List<Location> findAllByOrderByLocationNameAsc();
}