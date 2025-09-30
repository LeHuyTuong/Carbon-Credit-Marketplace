package com.carbonx.marketcarbon.service;



import com.carbonx.marketcarbon.dto.response.VehicleDetailResponse;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;

import java.util.List;

public interface VehicleService {
    // danh cho EV Owner
    public Long create(VehicleCreateRequest req);
    public Long update(Long id, VehicleUpdateRequest req)   ;
    public void delete(Long id);
    public List<Vehicle> getOwnerVehicles();

}

