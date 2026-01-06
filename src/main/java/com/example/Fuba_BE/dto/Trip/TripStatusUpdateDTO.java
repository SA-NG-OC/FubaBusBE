package com.example.Fuba_BE.dto.Trip;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripStatusUpdateDTO {
    @NotNull(message = "Status is required")
    private String status; // Waiting, Running, Completed, Delayed, Cancelled

    private String note;
}
