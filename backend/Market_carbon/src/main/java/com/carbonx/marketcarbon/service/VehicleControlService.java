package com.carbonx.marketcarbon.service;

import com.carbonx.marketcarbon.dto.response.PageResponse;
import com.carbonx.marketcarbon.dto.response.VehicleDetailResponse;

import java.util.List;

public interface VehicleControlService {

     List<VehicleDetailResponse> getAll();
     PageResponse<?> getAllVehiclesWithSortBy(int pageNo, int pageSize, String sortBy);
     PageResponse<?>  getAllVehiclesWithSortByMultipleColumns(int pageNo, int pageSize, String... sortBy);
     PageResponse<?>  getAllVehiclesWithSortByMultipleColumnsAndSearch(int pageNo, int pageSize, String search, String sortBy);
     PageResponse<?> getAllVehiclesOfCompanyWithSortBy(int pageNo, int pageSize, String sortBy);
}
