package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Route.IRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ApiResponse<Page<RouteResponseDTO>>> getRoutes(
            @RequestParam(required = false) String keyword,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "routeId",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        Page<RouteResponseDTO> result;

        if (keyword == null || keyword.trim().isEmpty()) {
            result = routeService.getAllRoutesForUI(pageable);
        } else {
            result = routeService.searchRoutes(keyword, pageable);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Routes fetched successfully", result)
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

    //Get RouteStop
    @GetMapping("/route-stop")
    public ResponseEntity<ApiResponse<List<RouteStopResponseDTO>>> getRouteStop()
    {
        List<RouteStopResponseDTO> result = routeService.getAllRouteStop();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "RouteStop fetched successfully", result)
        );
    }
}
