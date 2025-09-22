package com.carbonx.marketcarbon.service;



import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleCreateRequest req);
    List<VehicleResponse> getAll();
    VehicleResponse getById(Long id);
    VehicleResponse update(Long id, VehicleCreateRequest req);
    void delete(Long id);
}

