package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.TripCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TripCostRepository extends JpaRepository<TripCost, Integer> {

    // 1. Các hàm tính tổng (Giữ nguyên JPQL vì đơn giản và đúng chuẩn)
    @Query("SELECT SUM(tc.revenue) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(tc.totalCost) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(tc.profit) FROM TripCost tc WHERE tc.calculatedAt BETWEEN :start AND :end")
    BigDecimal sumProfitBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Query biểu đồ theo Thứ (Sửa thành NATIVE QUERY)
    // Trả về Object[]: index 0 là Label (String), index 1 là Revenue (BigDecimal)
    @Query(value = """
            SELECT 
                CAST(EXTRACT(ISODOW FROM t.departuretime) AS TEXT) as label, 
                SUM(tc.revenue) as value
            FROM tripcosts tc 
            JOIN trips t ON tc.tripid = t.tripid
            WHERE t.departuretime BETWEEN :start AND :end
            GROUP BY EXTRACT(ISODOW FROM t.departuretime)
            ORDER BY EXTRACT(ISODOW FROM t.departuretime)
            """, nativeQuery = true)
    List<Object[]> getRevenueByDayOfWeek(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 3. Query biểu đồ theo Khung giờ (Sửa thành NATIVE QUERY)
    @Query(value = """
            SELECT 
                CASE 
                    WHEN EXTRACT(HOUR FROM t.departuretime) BETWEEN 0 AND 11 THEN 'Morning'
                    WHEN EXTRACT(HOUR FROM t.departuretime) BETWEEN 12 AND 17 THEN 'Afternoon'
                    ELSE 'Evening'
                END as label,
                SUM(tc.revenue) as value
            FROM tripcosts tc 
            JOIN trips t ON tc.tripid = t.tripid
            WHERE t.departuretime BETWEEN :start AND :end
            GROUP BY 
                CASE 
                    WHEN EXTRACT(HOUR FROM t.departuretime) BETWEEN 0 AND 11 THEN 'Morning'
                    WHEN EXTRACT(HOUR FROM t.departuretime) BETWEEN 12 AND 17 THEN 'Afternoon'
                    ELSE 'Evening'
                END
            """, nativeQuery = true)
    List<Object[]> getRevenueByShift(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}