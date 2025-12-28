package com.example.Fuba_BE.exception;

import com.example.Fuba_BE.payload.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Bắt toàn bộ AppException
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<String>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ApiResponse<>(false, ex.getMessage(), null));
    }

    // Lỗi không xác định → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGlobalException(Exception ex) {
        return ResponseEntity
                .internalServerError()
                .body(new ApiResponse<>(false, "Internal Server Error", null));
    }
}
