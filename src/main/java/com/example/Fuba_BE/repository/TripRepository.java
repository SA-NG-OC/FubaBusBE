package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    // 1. Lấy danh sách ngày có chuyến đi (Giữ nguyên - Dùng cho Calendar)
    @Query("SELECT DISTINCT CAST(t.departureTime AS LocalDate) " +
            "FROM Trip t " +
            "WHERE t.departureTime >= :start AND t.departureTime <= :end")
    List<LocalDate> findDistinctTripDates(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // 2. Lấy chi tiết chuyến đi theo ngày (Giữ nguyên - Dùng cho view chi tiết ngày)
    @Query("SELECT t FROM Trip t " +
            "LEFT JOIN FETCH t.route r " +
            "LEFT JOIN FETCH r.origin " +
            "LEFT JOIN FETCH r.destination " +
            "LEFT JOIN FETCH t.vehicle v " +
            "LEFT JOIN FETCH v.vehicleType " +
            "LEFT JOIN FETCH t.driver d " +
            "LEFT JOIN FETCH d.user " +
            "WHERE t.departureTime >= :startOfDay AND t.departureTime <= :endOfDay " +
            "ORDER BY t.departureTime ASC")
    List<Trip> findAllTripsByDate(@Param("startOfDay") LocalDateTime startOfDay,
                                  @Param("endOfDay") LocalDateTime endOfDay);

    // 🔥 3. HÀM QUAN TRỌNG NHẤT: Tìm kiếm tổng hợp + Phân trang + Eager Load (Khắc phục chậm)
    // Logic: Nếu tham số truyền vào là NULL thì bỏ qua điều kiện đó (:param IS NULL)
    // 🔥 FIX LỖI: Thêm CAST(... AS type) để PostgreSQL nhận diện được kiểu dữ liệu khi param bị NULL
    @Query(value = "SELECT t FROM Trip t " +
            "LEFT JOIN FETCH t.route r " +
            "LEFT JOIN FETCH r.origin " +
            "LEFT JOIN FETCH r.destination " +
            "LEFT JOIN FETCH t.vehicle v " +
            "LEFT JOIN FETCH v.vehicleType " +
            "LEFT JOIN FETCH t.driver d " +
            "LEFT JOIN FETCH d.user " +
            "WHERE (CAST(:status AS string) IS NULL OR t.status = :status) " +
            "AND (CAST(:start AS timestamp) IS NULL OR t.departureTime >= :start) " +
            "AND (CAST(:end AS timestamp) IS NULL OR t.departureTime <= :end)",

            // Count Query cũng phải CAST tương tự
            countQuery = "SELECT count(t) FROM Trip t " +
                    "WHERE (CAST(:status AS string) IS NULL OR t.status = :status) " +
                    "AND (CAST(:start AS timestamp) IS NULL OR t.departureTime >= :start) " +
                    "AND (CAST(:end AS timestamp) IS NULL OR t.departureTime <= :end)")
    Page<Trip> findTripsWithFilter(
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // 4. Update status (Giữ nguyên)
    @Modifying
    @Query("UPDATE Trip t SET t.status = :status WHERE t.id = :tripId")
    int updateStatus(@Param("tripId") Integer tripId, @Param("status") String status);
}