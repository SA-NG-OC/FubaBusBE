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

    @Query("SELECT SUM(b.totalAmount) FROM Booking b " +
            "WHERE b.bookingStatus IN ('Paid', 'Completed') " +
            "AND b.createdAt BETWEEN :start AND :end")
    BigDecimal sumRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // [THAY ĐỔI] Lấy doanh thu 12 tháng gần nhất (Rolling 12 months)
    // Sắp xếp theo Năm-Tháng để biểu đồ hiển thị đúng thứ tự thời gian
    @Query(value = """
        SELECT TO_CHAR(createdat, 'Mon-YY'), SUM(totalamount)
        FROM bookings
        WHERE bookingstatus IN ('Paid', 'Completed')
        AND createdat >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '11 months')
        GROUP BY TO_CHAR(createdat, 'Mon-YY'), DATE_TRUNC('month', createdat)
        ORDER BY DATE_TRUNC('month', createdat) ASC
    """, nativeQuery = true)
    List<Object[]> getRevenueLast12Months();
}