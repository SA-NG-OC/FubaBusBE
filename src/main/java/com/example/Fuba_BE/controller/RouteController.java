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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Tag(name = "Route Management", description = "APIs for managing bus routes")
public class RouteController {

    private final IRouteService routeService;

    @GetMapping
    @Operation(summary = "Get all routes", description = "Retrieve routes with optional search keyword and pagination")
    public ResponseEntity<ApiResponse<Page<RouteResponseDTO>>> getRoutes(
            @Parameter(description = "Search keyword")
            @RequestParam(required = false) String keyword,
            
            @Parameter(hidden = true)
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
    @Operation(summary = "Get routes for selection dropdown", description = "Retrieve simplified route list for dropdowns")
    public ResponseEntity<ApiResponse<List<RouteSelectionDTO>>> getRoutesForSelection() {
        List<RouteSelectionDTO> routes = routeService.getAllRoutesForSelection();
        return ResponseEntity.ok(ApiResponse.success("Route selection list retrieved", routes));
    }

    @PostMapping
    @Operation(summary = "Create new route", description = "Create a new bus route")
    public ResponseEntity<ApiResponse<RouteResponseDTO>> createRoute(
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Route created successfully", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update route", description = "Update an existing route")
    public ResponseEntity<ApiResponse<RouteResponseDTO>> updateRoute(
            @Parameter(description = "Route ID", required = true)
            @PathVariable Integer id,
            
            @Valid @RequestBody RouteRequestDTO request
    ) {
        RouteResponseDTO response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success("Route updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete route", description = "Delete a route by ID")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @Parameter(description = "Route ID", required = true)
            @PathVariable Integer id
    ) {
        routeService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deleted successfully", null));
    }

    @GetMapping("/route-stop")
    @Operation(summary = "Get all route stops", description = "Retrieve all route stops")
    public ResponseEntity<ApiResponse<List<RouteStopResponseDTO>>> getRouteStop() {
        List<RouteStopResponseDTO> result = routeService.getAllRouteStop();
        return ResponseEntity.ok(ApiResponse.success("Route stops retrieved successfully", result));
    }
}
