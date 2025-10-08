package com.carbonx.marketcarbon.service;



import com.carbonx.marketcarbon.dto.response.VehicleResponse;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;

import java.util.List;

public interface VehicleService {
    // danh cho EV Owner
     VehicleResponse create(VehicleCreateRequest req);
     VehicleResponse update(Long id, VehicleUpdateRequest req)   ;
     void delete(Long id);
     List<VehicleResponse> getOwnerVehicles();

}

