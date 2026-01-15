package com.example.Fuba_BE.service.TripTracking;

import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;
import com.example.Fuba_BE.dto.TripTracking.TripRouteResponse;

public interface ITrackingService {
    void saveLocationHistory(LocationUpdateReq req);
    TripRouteResponse getTripRouteInfo(Integer tripId);
}
