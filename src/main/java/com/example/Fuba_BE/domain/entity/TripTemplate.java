package com.example.Fuba_BE.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "triptemplates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "templateid")
    private Integer templateId;

    @Column(name = "templatename", nullable = false)
    private String templateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routeid", nullable = false)
    private Route route;

    @Column(name = "departuretime", nullable = false)
    private LocalTime departureTime;

    @Column(name = "daysofweek", nullable = false)
    private String daysOfWeek; // Format: "Mon,Tue,Wed,Thu,Fri" or "Daily" or "Sat,Sun"

    @Column(name = "baseprice", nullable = false)
    private BigDecimal basePrice;

    @Column(name = "onlinebookingcutoff")
    @Builder.Default
    private Integer onlineBookingCutoff = 60;

    @Column(name = "minpassengers")
    @Builder.Default
    private Integer minPassengers = 1;

    @Column(name = "maxpassengers")
    @Builder.Default
    private Integer maxPassengers = 40;

    // ========== ROUND-TRIP & INTERVAL CONFIGURATION ==========
    
    @Column(name = "generateroundtrip", nullable = false)
    @Builder.Default
    private Boolean generateRoundTrip = false; // TRUE = Tạo cả chuyến đi và về

    @Column(name = "intervalminutes")
    @Builder.Default
    private Integer intervalMinutes = 0; // Tạo trips cách nhau N phút (0 = chỉ tạo 1 trip/ngày)

    @Column(name = "tripsperday")
    @Builder.Default
    private Integer tripsPerDay = 1; // Số trips tạo mỗi ngày (nếu intervalMinutes > 0)

    @Column(name = "maxgenerationdays")
    @Builder.Default
    private Integer maxGenerationDays = 31; // Tối đa số ngày có thể generate (default 1 tháng)

    @Column(name = "autoassigndriver")
    @Builder.Default
    private Boolean autoAssignDriver = true; // Tự động assign driver khi generate

    @Column(name = "autoassignvehicle")
    @Builder.Default
    private Boolean autoAssignVehicle = true; // Tự động assign vehicle khi generate

    @Column(name = "autocancelifnotenough")
    @Builder.Default
    private Boolean autoCancelIfNotEnough = false;

    @Column(name = "isactive")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "effectivefrom", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effectiveto")
    private LocalDate effectiveTo;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "createdby")
    private User createdBy;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if template is currently effective
     */
    public boolean isCurrentlyEffective() {
        if (!isActive) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        if (today.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && today.isAfter(effectiveTo)) {
            return false;
        }
        
        return true;
    }

    /**
     * Check if template applies to a specific date
     */
    public boolean appliesToDate(LocalDate date) {
        if (!isCurrentlyEffective()) {
            return false;
        }
        
        // Check if date is within effective range
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        
        if (effectiveTo != null && date.isAfter(effectiveTo)) {
            return false;
        }
        
        // Check day of week
        String dayOfWeek = date.getDayOfWeek().name().substring(0, 3); // MON, TUE, WED...
        dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1).toLowerCase(); // Mon, Tue, Wed...
        
        if ("Daily".equalsIgnoreCase(daysOfWeek)) {
            return true;
        }
        
        if ("Weekends".equalsIgnoreCase(daysOfWeek)) {
            return dayOfWeek.equals("Sat") || dayOfWeek.equals("Sun");
        }
        
        if ("Weekdays".equalsIgnoreCase(daysOfWeek)) {
            return !dayOfWeek.equals("Sat") && !dayOfWeek.equals("Sun");
        }
        
        // Check if day is in comma-separated list
        final String targetDay = dayOfWeek; // Make effectively final for lambda
        List<String> days = Arrays.asList(daysOfWeek.split(","));
        return days.stream().anyMatch(d -> d.trim().equalsIgnoreCase(targetDay));
    }

    /**
     * Get list of day names
     */
    public List<String> getDaysList() {
        if ("Daily".equalsIgnoreCase(daysOfWeek)) {
            return Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        }
        
        if ("Weekends".equalsIgnoreCase(daysOfWeek)) {
            return Arrays.asList("Sat", "Sun");
        }
        
        if ("Weekdays".equalsIgnoreCase(daysOfWeek)) {
            return Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri");
        }
        
        return Arrays.asList(daysOfWeek.split(","));
    }

    // ========== ROUND-TRIP & INTERVAL LOGIC ==========

    /**
     * Validate generation request không vượt quá maxGenerationDays
     */
    public boolean isValidGenerationPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        if (endDate.isBefore(startDate)) {
            return false;
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1; // +1 to include both start and end
        return daysBetween <= maxGenerationDays;
    }

    /**
     * Tính tổng số trips sẽ tạo trong 1 ngày (bao gồm round-trip)
     */
    public int getTotalTripsPerDay() {
        int base = Math.max(1, tripsPerDay != null ? tripsPerDay : 1);
        return (generateRoundTrip != null && generateRoundTrip) ? base * 2 : base;
    }

    /**
     * Get danh sách departure times cho 1 ngày (dựa trên interval)
     * Trả về các thời gian xuất phát cho chuyến ĐI (không bao gồm chuyến về)
     */
    public List<LocalTime> getDepartureTimesForDay() {
        if (intervalMinutes == null || intervalMinutes <= 0 || tripsPerDay == null || tripsPerDay <= 1) {
            return List.of(departureTime);
        }

        List<LocalTime> times = new java.util.ArrayList<>();
        LocalTime currentTime = departureTime;
        for (int i = 0; i < tripsPerDay; i++) {
            times.add(currentTime);
            if (i < tripsPerDay - 1) { // Don't add interval after last trip
                currentTime = currentTime.plusMinutes(intervalMinutes);
            }
        }
        return times;
    }

    /**
     * Check if should generate round-trip (chuyến về)
     */
    public boolean shouldGenerateRoundTrip() {
        return generateRoundTrip != null && generateRoundTrip;
    }

    /**
     * Get interval between trips (in minutes)
     */
    public int getIntervalMinutes() {
        return intervalMinutes != null ? intervalMinutes : 0;
    }
}
