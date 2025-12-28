package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Route.IRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class RouteController {

    private final IRouteService routeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RouteResponseDTO>>> getAllRoutes() {
        List<RouteResponseDTO> routes = routeService.getAllRoutesForUI();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Routes fetched successfully", routes)
        );
    }

    // Create
    @PostMapping
    public ResponseEntity<ApiResponse<RouteResponseDTO>> createRoute(
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Route created successfully", response));
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponseDTO>> updateRoute(
            @PathVariable Integer id,
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Route updated successfully", response)
        );
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Integer id) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Route deleted successfully", null)
        );
    }
}
