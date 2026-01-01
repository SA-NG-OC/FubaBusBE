package com.example.Fuba_BE.dto.Trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TripCalendarDTO {
    private Integer tripId;
    private LocalDateTime start;   // Giờ khởi hành (quan trọng nhất để map lên lịch)
    private String title;          // VD: "SG -> ĐL" (để hiển thị tooltip nếu cần)
}