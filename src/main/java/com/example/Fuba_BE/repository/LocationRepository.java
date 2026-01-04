package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {

    // Tìm kiếm tương đối (cho search box)
    List<Location> findByLocationNameContainingIgnoreCase(String keyword);

    // Tìm kiếm chính xác để map dữ liệu từ DTO (Mới thêm)
    Optional<Location> findByLocationName(String locationName);

    // ... các method cũ giữ nguyên
    List<Location> findAllByOrderByLocationNameAsc();

    @Query("""
        SELECT new com.example.Fuba_BE.dto.Location.LocationResponseDTO(
            l.locationId,
            l.locationName
        )
        FROM Location l
        ORDER BY l.locationName
    """)
    List<LocationResponseDTO> findAllBasic();
}