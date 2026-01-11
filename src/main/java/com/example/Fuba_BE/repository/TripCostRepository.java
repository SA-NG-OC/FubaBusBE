package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.TripCost;
import com.example.Fuba_BE.dto.AdminReport.ChartDataRes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripCostRepository extends JpaRepository<TripCost, Integer> {

    // 1. Tổng hợp Doanh thu, Chi phí, Lợi nhuận trong khoảng thời gian
    @Query("SELECT SUM(tc.revenue) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(tc.totalCost) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(tc.profit) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumProfitBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Query biểu đồ Doanh thu theo Ngày trong tuần (Sử dụng function của DB để lấy thứ)
    // Lưu ý: function DAYOFWEEK/ISODOW tùy thuộc vào DB (PostgreSQL dùng function native khác H2/MySQL)
    // Ở đây tôi dùng generic approach, hoặc bạn có thể group by Java.
    // Dưới đây là ví dụ tương thích PostgreSQL (extract dow)
    @Query(value = "SELECT new com.example.Fuba_BE.domain.dto.response.ChartDataRes(" +
            "CAST(FUNCTION('extract', 'isodow', t.departureTime) AS string), SUM(tc.revenue)) " +
            "FROM TripCost tc JOIN tc.trip t " +
            "WHERE t.departureTime BETWEEN :start AND :end " +
            "GROUP BY FUNCTION('extract', 'isodow', t.departureTime) " +
            "ORDER BY FUNCTION('extract', 'isodow', t.departureTime)")
    List<ChartDataRes> getRevenueByDayOfWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. Query biểu đồ Doanh thu theo Khung giờ (Sáng, Trưa, Tối)
    @Query("SELECT new com.example.Fuba_BE.domain.dto.response.ChartDataRes(" +
            "CASE " +
            "  WHEN FUNCTION('hour', t.departureTime) BETWEEN 0 AND 11 THEN 'Morning' " +
            "  WHEN FUNCTION('hour', t.departureTime) BETWEEN 12 AND 17 THEN 'Afternoon' " +
            "  ELSE 'Evening' " +
            "END, " +
            "SUM(tc.revenue)) " +
            "FROM TripCost tc JOIN tc.trip t " +
            "WHERE t.departureTime BETWEEN :start AND :end " +
            "GROUP BY " +
            "CASE " +
            "  WHEN FUNCTION('hour', t.departureTime) BETWEEN 0 AND 11 THEN 'Morning' " +
            "  WHEN FUNCTION('hour', t.departureTime) BETWEEN 12 AND 17 THEN 'Afternoon' " +
            "  ELSE 'Evening' " +
            "END")
    List<ChartDataRes> getRevenueByShift(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}