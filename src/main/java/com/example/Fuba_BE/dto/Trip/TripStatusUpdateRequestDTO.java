package com.example.Fuba_BE.dto.Trip;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TripStatusUpdateRequestDTO {

    @NotBlank(message = "Status cannot be left blank.")
    private String status;
}
