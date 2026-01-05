package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // 1. Tính tổng doanh thu trong khoảng thời gian (Status: Paid, Completed)
    @Query("SELECT SUM(b.totalAmount) FROM Booking b " +
            "WHERE b.bookingStatus IN ('Paid', 'Completed') " +
            "AND b.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 2. Query cho biểu đồ đường: Doanh thu theo tháng trong năm hiện tại
    // Trả về: [Tháng (String - Jan, Feb...), Doanh thu (BigDecimal)]
    @Query(value = """
        SELECT TO_CHAR(createdat, 'Mon'), SUM(totalamount)
        FROM bookings
        WHERE bookingstatus IN ('Paid', 'Completed')
        AND EXTRACT(YEAR FROM createdat) = :year
        GROUP BY TO_CHAR(createdat, 'Mon'), EXTRACT(MONTH FROM createdat)
        ORDER BY EXTRACT(MONTH FROM createdat) ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueTrends(@Param("year") int year);
}
