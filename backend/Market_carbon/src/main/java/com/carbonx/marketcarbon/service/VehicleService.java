package com.carbonx.marketcarbon.service;



import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;

import java.util.List;

public interface VehicleService {
    public Long create(VehicleCreateRequest req);
    public List<Vehicle> getAll();
    public List<Vehicle> getByPlateNumber(String number);
    public Long update(Long id, VehicleUpdateRequest req);
    public void delete(Long id);
}

