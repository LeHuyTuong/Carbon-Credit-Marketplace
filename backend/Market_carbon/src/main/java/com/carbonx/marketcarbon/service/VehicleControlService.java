package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.PageResponse;
import com.carbonx.marketcarbon.dto.response.VehicleDetailResponse;

import java.util.List;

public interface VehicleControlService {

    public List<VehicleDetailResponse> getAll();
    public PageResponse<?> getAllVehiclesWithSortBy(int pageNo, int pageSize, String sortBy);
    public PageResponse<?>  getAllVehiclesWithSortByMultipleColumns(int pageNo, int pageSize, String... sortBy);
    public PageResponse<?>  getAllVehiclesWithSortByMultipleColumnsAndSearch(int pageNo, int pageSize, String search, String sortBy);
}
