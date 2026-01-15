package com.example.Fuba_BE.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.Fuba_BE.domain.entity.DriverWorkLog;

@Repository
public interface DriverWorkLogRepository extends JpaRepository<DriverWorkLog, Integer> {

    /**
     * Find work log by driver and work date
     */
    @Query("SELECT dwl FROM DriverWorkLog dwl WHERE dwl.driver.driverId = :driverId AND dwl.workDate = :workDate")
    Optional<DriverWorkLog> findByDriverAndWorkDate(@Param("driverId") Integer driverId, @Param("workDate") LocalDate workDate);

    /**
     * Get all work logs for a driver in date range
     */
    @Query("SELECT dwl FROM DriverWorkLog dwl WHERE dwl.driver.driverId = :driverId AND dwl.workDate BETWEEN :startDate AND :endDate ORDER BY dwl.workDate DESC")
    List<DriverWorkLog> findByDriverAndDateRange(@Param("driverId") Integer driverId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get total working hours for a driver on specific date
     */
    @Query("SELECT COALESCE(SUM(dwl.totalHours), 0) FROM DriverWorkLog dwl WHERE dwl.driver.driverId = :driverId AND dwl.workDate = :workDate")
    Double getTotalHoursByDriverAndDate(@Param("driverId") Integer driverId, @Param("workDate") LocalDate workDate);

    /**
     * Check if driver has exceeded maximum hours on date
     */
    default boolean hasExceededMaxHours(Integer driverId, LocalDate workDate, double maxHours) {
        Double totalHours = getTotalHoursByDriverAndDate(driverId, workDate);
        return totalHours != null && totalHours >= maxHours;
    }

    /**
     * Get work logs for multiple drivers on specific date
     */
    @Query("SELECT dwl FROM DriverWorkLog dwl WHERE dwl.driver.driverId IN :driverIds AND dwl.workDate = :workDate")
    List<DriverWorkLog> findByDriversAndDate(@Param("driverIds") List<Integer> driverIds, @Param("workDate") LocalDate workDate);

    /**
     * Get recent work logs for dashboard/reports
     */
    @Query("SELECT dwl FROM DriverWorkLog dwl WHERE dwl.workDate >= :since ORDER BY dwl.workDate DESC, dwl.startTime DESC")
    List<DriverWorkLog> findRecentLogs(@Param("since") LocalDate since);
}
