package com.example.Fuba_BE.service.TripTracking;

import com.example.Fuba_BE.dto.TripTracking.LocationUpdateReq;

public interface ITrackingService {
    void saveLocationHistory(LocationUpdateReq req);
}
