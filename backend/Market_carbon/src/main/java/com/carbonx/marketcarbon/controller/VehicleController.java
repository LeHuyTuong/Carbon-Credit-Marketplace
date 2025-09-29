package com.carbonx.marketcarbon.controller;




import com.carbonx.marketcarbon.domain.StatusCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.response.VehicleResponse;
import com.carbonx.marketcarbon.service.VehicleService;
import com.carbonx.marketcarbon.utils.CommonRequest;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<TuongCommonResponse<Long>> create(
            @Valid @RequestBody TuongCommonRequest<@Valid VehicleCreateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long created = vehicleService.create(req.getData());

        try{
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace,now, rs, created);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace,now, rs, created);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<Vehicle>>> getAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime){

        // check not found
        List<Vehicle> vehicles = vehicleService.getAll();
        if(vehicles == null || vehicles.isEmpty()){
            throw new ResourceNotFoundException("Vehicle not found", requestTrace, requestDateTime);
        }

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        try{
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<List<Vehicle>> response = new TuongCommonResponse<>(trace,now, rs, vehicles);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<List<Vehicle>> response = new TuongCommonResponse<>(trace,now, rs, vehicles);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/by-plate/{plateNumber}")
    public ResponseEntity<TuongCommonResponse<List<Vehicle>>> getByPlateNumber(
            @PathVariable("plateNumber") String plateNumber,
            @RequestHeader(value = "X-Request-Trace",required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){

        String trace =  requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<Vehicle> list = vehicleService.getByPlateNumber(plateNumber);

        try{
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<List<Vehicle>> response = new TuongCommonResponse<>(trace,now, rs, list);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<List<Vehicle>> response = new TuongCommonResponse<>(trace,now, rs, list);
            return ResponseEntity.ok(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Long>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<VehicleUpdateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
        ){

        String trace =  requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long updated = vehicleService.update(id, req.getData());

        try{
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace,now, rs, updated);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace,now, rs, updated);
            return ResponseEntity.ok(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        vehicleService.delete(id);
        try{
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace,now, rs, null);
            return ResponseEntity.ok(response);
        }catch(Exception e){
            log.info("errorMessage = {}", e.getMessage(), e.getCause());
            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.INTERNAL_SERVER_ERROR.getCode(),
                    StatusCode.INTERNAL_SERVER_ERROR.getMessage());
            TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace,now, rs,null);
            return ResponseEntity.ok(response);
        }
    }
}
