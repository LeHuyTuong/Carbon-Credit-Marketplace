package com.carbonx.marketcarbon.controller;




import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;
import com.carbonx.marketcarbon.service.VehicleService;
import com.carbonx.marketcarbon.utils.CommonRequest;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<CommonResponse<VehicleResponse>> create(
            @Valid @RequestBody CommonRequest<VehicleCreateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        VehicleResponse created = vehicleService.create(req.getRequestParamters());

        return ResponseEntity.ok(ResponseUtil.success(trace, created));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<VehicleResponse>>> getAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime){

        // check not found
        List<VehicleResponse> vehicles = vehicleService.getAll();
        if(vehicles == null || vehicles.isEmpty()){
            throw new ResourceNotFoundException("Vehicle not found", requestTrace, requestDateTime);
        }

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        return ResponseEntity.ok(ResponseUtil.success(trace, vehicles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VehicleResponse>> getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Trace",required = false) String requestTrace){

        String trace =  requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        VehicleResponse vehicle = vehicleService.getById(id);

        return ResponseEntity.ok(ResponseUtil.success(trace, vehicle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VehicleResponse>> update(
            @PathVariable Long id,
            @RequestBody CommonRequest<VehicleCreateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        VehicleResponse updated = vehicleService.update(id, req.getRequestParamters());

        return ResponseEntity.ok(ResponseUtil.success(trace, updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<String>> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace ){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        vehicleService.delete(id);
        return ResponseEntity.ok(ResponseUtil.success(trace,"Delete Success"));
    }
}
