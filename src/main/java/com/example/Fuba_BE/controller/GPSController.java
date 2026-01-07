package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;
import com.example.Fuba_BE.service.TripTracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
}
