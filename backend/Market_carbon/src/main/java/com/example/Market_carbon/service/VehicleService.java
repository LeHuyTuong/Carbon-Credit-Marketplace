package com.example.Market_carbon.service;

import com.example.Market_carbon.model.Vehicle;
import com.example.Market_carbon.request.VehicleCreateRequest;
import com.example.Market_carbon.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleCreateRequest req);
    List<VehicleResponse> getAll();
    VehicleResponse getById(Long id);
    VehicleResponse update(Long id, VehicleCreateRequest req);
    void delete(Long id);
}

