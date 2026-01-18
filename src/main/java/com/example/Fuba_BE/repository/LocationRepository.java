package com.example.Fuba_BE.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.Location;
import com.example.Fuba_BE.dto.Location.LocationResponseDTO;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {

    // Tìm kiếm tương đối (cho search box)
    List<Location> findByLocationNameContainingIgnoreCase(String keyword);

    // Tìm kiếm chính xác để map dữ liệu từ DTO (Mới thêm)
    Optional<Location> findByLocationName(String locationName);

    // Pagination và search methods
    Page<Location> findByLocationNameContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String locationName, String address, Pageable pageable);

    Page<Location> findByProvinceContainingIgnoreCase(String province, Pageable pageable);

    Page<Location> findByLocationNameContainingIgnoreCaseAndProvinceContainingIgnoreCase(
            String locationName, String province, Pageable pageable);

    // ... các method cũ giữ nguyên
    List<Location> findAllByOrderByLocationNameAsc();

    @Query("""
                SELECT new com.example.Fuba_BE.dto.Location.LocationResponseDTO(
                    l.locationId,
                    l.locationName,
                    l.address,
                    l.province,
                    l.latitude,
                    l.longitude,
                    l.createdAt
                )
                FROM Location l
                ORDER BY l.locationName
            """)
    List<LocationResponseDTO> findAllBasic();

    @Query("SELECT DISTINCT l.province FROM Location l WHERE l.province IS NOT NULL ORDER BY l.province")
    List<String> findDistinctProvinces();
}