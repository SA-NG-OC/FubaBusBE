package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;
import com.example.Fuba_BE.dto.TripTracking.TripRouteResponse;
import com.example.Fuba_BE.payload.ApiResponse;
import com.example.Fuba_BE.service.TripTracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class GPSController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TrackingService trackingService;

    @MessageMapping("/gps/update")
    public void receiveLocation(@Payload LocationUpdateReq locationData) {

        String destination = "/topic/trip/" + locationData.getTripId();
        messagingTemplate.convertAndSend(destination, locationData);

        trackingService.saveLocationHistory(locationData);
    }

    @GetMapping("/gps/{tripId}/route-info")
    @ResponseBody
    public ResponseEntity<ApiResponse<TripRouteResponse>> getTripRouteInfo(@PathVariable Integer tripId) {

        TripRouteResponse data = trackingService.getTripRouteInfo(tripId);

        return ResponseEntity.ok(
                ApiResponse.success("Get route information successfully", data)
        );
    }
}
