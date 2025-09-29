package com.carbonx.marketcarbon.service;



import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleCreateRequest req);
    List<VehicleResponse> getAll();
    VehicleResponse getByPlateNumber(String number);
    VehicleResponse update(Long id, VehicleUpdateRequest req);
    void delete(Long id);
}

