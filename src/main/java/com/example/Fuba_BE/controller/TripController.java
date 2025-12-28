package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.domain.Trip;
import com.example.Fuba_BE.service.Trip.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trips")
@RequiredArgsConstructor
public class TripController {
    @Autowired
    private TripService tripService; // Gọi ông Quản lý ra để chờ lệnh

    // API 1: Lấy danh sách chuyến xe
    // Phương thức: GET
    // Đường dẫn: http://localhost:8080/trips
    @GetMapping
    public List<Trip> getAllTrips() {
        return tripService.getAllTrips();
    }

    // API 2: Tạo chuyến xe mới
    // Phương thức: POST
    // Đường dẫn: http://localhost:8080/api/trips
    @PostMapping
    public Trip createTrip(@RequestBody Trip trip) {
        // @RequestBody: Dịch bức thư JSON từ khách gửi thành Object Java
        return tripService.createTrip(trip);
    }
}
