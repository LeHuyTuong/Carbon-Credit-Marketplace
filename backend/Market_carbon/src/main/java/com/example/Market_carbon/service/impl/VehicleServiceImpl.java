package com.example.Market_carbon.service.impl;

import com.example.Market_carbon.request.VehicleCreateRequest;
import com.example.Market_carbon.response.VehicleResponse;
import com.example.Market_carbon.service.VehicleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {
    @Override
    public VehicleResponse create(VehicleCreateRequest req) {
        return null;
    }

    @Override
    public List<VehicleResponse> getAll() {
        return List.of();
    }

    @Override
    public VehicleResponse getById(Long id) {
        return null;
    }

    @Override
    public VehicleResponse update(Long id, VehicleCreateRequest req) {
        return null;
    }

    @Override
    public void delete(Long id) {

    }
}
