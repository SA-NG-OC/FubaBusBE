package com.example.Fuba_BE.service.Route;

import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import com.example.Fuba_BE.dto.Routes.RouteSelectionDTO;
import com.example.Fuba_BE.dto.Routes.RouteStopResponseDTO;
import com.example.Fuba_BE.dto.Trip.TripDetailedResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IRouteService {

    RouteResponseDTO createRoute(RouteRequestDTO request);

    RouteResponseDTO updateRoute(Integer routeId, RouteRequestDTO request);

    void deleteRoute(Integer routeId);

    List<RouteStopResponseDTO> getAllRouteStop();

    Page<RouteResponseDTO> getAllRoutesForUI(Pageable pageable);

    Page<RouteResponseDTO> searchRoutes(String keyword, Pageable pageable);

    List<RouteSelectionDTO> getAllRoutesForSelection();
}
