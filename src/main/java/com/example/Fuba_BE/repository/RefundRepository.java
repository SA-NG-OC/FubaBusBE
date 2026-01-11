package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {

    /**
     * Find all refunds for a booking
     */
    List<Refund> findByBookingBookingId(Integer bookingId);

    /**
     * Find refund by booking ID and status
     */
    Optional<Refund> findByBookingBookingIdAndRefundStatus(Integer bookingId, String status);

    /**
     * Sum total refund amount between dates (only Refunded status)
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r " +
            "WHERE r.refundStatus = 'Refunded' " +
            "AND r.createdAt BETWEEN :start AND :end")
    BigDecimal sumRefundAmountBetween(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /**
     * Sum refund for specific booking status (for report filtering)
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r " +
            "WHERE r.refundStatus = 'Refunded' " +
            "AND r.booking.bookingStatus = :bookingStatus " +
            "AND r.createdAt BETWEEN :start AND :end")
    BigDecimal sumRefundByBookingStatus(@Param("bookingStatus") String bookingStatus,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    /**
     * Count refunds by status
     */
    long countByRefundStatus(String status);

    /**
     * Find pending refunds (for processing)
     */
    List<Refund> findByRefundStatusOrderByCreatedAtAsc(String status);
}
