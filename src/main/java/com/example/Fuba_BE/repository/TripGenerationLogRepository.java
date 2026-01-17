package com.example.Fuba_BE.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.TripGenerationLog;

@Repository
public interface TripGenerationLogRepository extends JpaRepository<TripGenerationLog, Integer> {

    /**
     * Find logs for a specific template
     */
    @Query("SELECT tgl FROM TripGenerationLog tgl " +
           "LEFT JOIN FETCH tgl.template tt " +
           "LEFT JOIN FETCH tgl.generatedBy u " +
           "WHERE tgl.template.templateId = :templateId " +
           "ORDER BY tgl.createdAt DESC")
    List<TripGenerationLog> findByTemplateId(@Param("templateId") Integer templateId);

    /**
     * Find recent generation logs
     */
    @Query("SELECT tgl FROM TripGenerationLog tgl " +
           "LEFT JOIN FETCH tgl.template tt " +
           "LEFT JOIN FETCH tgl.generatedBy u " +
           "WHERE tgl.createdAt >= :since " +
           "ORDER BY tgl.createdAt DESC")
    List<TripGenerationLog> findRecentLogs(@Param("since") LocalDateTime since);

    /**
     * Find logs by status
     */
    @Query("SELECT tgl FROM TripGenerationLog tgl " +
           "LEFT JOIN FETCH tgl.template tt " +
           "WHERE tgl.status = :status " +
           "ORDER BY tgl.createdAt DESC")
    List<TripGenerationLog> findByStatus(@Param("status") String status);

    /**
     * Find logs by date range
     */
    @Query("SELECT tgl FROM TripGenerationLog tgl " +
           "LEFT JOIN FETCH tgl.template tt " +
           "WHERE tgl.startDate >= :startDate " +
           "AND tgl.endDate <= :endDate " +
           "ORDER BY tgl.createdAt DESC")
    List<TripGenerationLog> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Get generation statistics
     */
    @Query("SELECT " +
           "COUNT(tgl), " +
           "SUM(tgl.totalTripsCreated), " +
           "SUM(tgl.totalTripsSkipped), " +
           "AVG(tgl.executionTime) " +
           "FROM TripGenerationLog tgl " +
           "WHERE tgl.createdAt >= :since")
    Object[] getStatisticsSince(@Param("since") LocalDateTime since);
}
