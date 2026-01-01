package com.example.Fuba_BE.service.Route;

import com.example.Fuba_BE.dto.Routes.RouteRequestDTO;
import com.example.Fuba_BE.dto.Routes.RouteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IRouteService {

    RouteResponseDTO createRoute(RouteRequestDTO request);

    RouteResponseDTO updateRoute(Integer routeId, RouteRequestDTO request);

    void deleteRoute(Integer routeId);

    Page<RouteResponseDTO> getAllRoutesForUI(Pageable pageable);

    Page<RouteResponseDTO> searchRoutes(String keyword, Pageable pageable);

}
