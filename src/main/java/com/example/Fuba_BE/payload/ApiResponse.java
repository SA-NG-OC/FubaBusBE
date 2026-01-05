package com.example.Fuba_BE.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {
    
    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;
    
    @Schema(description = "Human-readable message describing the result", example = "Operation completed successfully")
    private String message;
    
    @Schema(description = "Response data payload")
    private T data;
    
    @Schema(description = "Error code for failed operations", example = "NOT_FOUND")
    private String errorCode;
    
    @Schema(description = "Timestamp of the response", example = "2026-01-05T14:30:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    // Factory methods for convenience
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return success("Operation successful", data);
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, T data, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }
}