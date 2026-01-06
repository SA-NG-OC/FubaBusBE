package com.example.Fuba_BE.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.dto.Routes.RouteSelectionDTO;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.Route.IRouteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

        return ResponseEntity.ok(ApiResponse.success("Routes retrieved successfully", result));
    }

    @GetMapping("/selection")
    public ResponseEntity<ApiResponse<List<RouteSelectionDTO>>> getRoutesForSelection() {
        List<RouteSelectionDTO> routes = routeService.getAllRoutesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Route selection list retrieved", routes));
    }

        @PostMapping
    public ResponseEntity<ApiResponse<RouteResponseDTO>> createRoute(
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route created successfully", response));
    }

        @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteResponseDTO>> updateRoute(
            @PathVariable Integer id,
            
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Route updated successfully", response));
    }

        @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable Integer id
    ) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deleted successfully", null));
    }

    @GetMapping("/route-stop")
    public ResponseEntity<ApiResponse<List<RouteStopResponseDTO>>> getRouteStop() {
        List<RouteStopResponseDTO> result = routeService.getAllRouteStop();
        return ResponseEntity.ok(ApiResponse.success("Route stops retrieved successfully", result));
    }
}
