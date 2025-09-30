package com.carbonx.marketcarbon.controller;


import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.response.PageResponse;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.model.KycProfile;
import com.carbonx.marketcarbon.model.Vehicle;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.service.VehicleControlService;
import com.carbonx.marketcarbon.service.VehicleService;
import com.carbonx.marketcarbon.utils.CommonRequest;
import com.carbonx.marketcarbon.utils.CommonResponse;
import com.carbonx.marketcarbon.utils.ResponseUtil;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
    private final VehicleControlService  vehicleControlService;

    @PostMapping
    public ResponseEntity<TuongCommonResponse<Long>> create(
            @Valid @RequestBody TuongCommonRequest<@Valid VehicleCreateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        Long created = vehicleService.create(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, created);
        return ResponseEntity.ok(response);

    }

    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<Vehicle>>> getAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        // check not found
        List<Vehicle> vehicles = vehicleService.getOwnerVehicles();
        if (vehicles == null || vehicles.isEmpty()) {
            throw new ResourceNotFoundException("Vehicle not found", requestTrace, requestDateTime);
        }

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<Vehicle>> response = new TuongCommonResponse<>(trace, now, rs, vehicles);
        return ResponseEntity.ok(response);

    }


    @PutMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Long>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<VehicleUpdateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Long updated = vehicleService.update(id, req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Long> response = new TuongCommonResponse<>(trace, now, rs, updated);
        return ResponseEntity.ok(response);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        vehicleService.delete(id);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<TuongCommonResponse<?>> getAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @Min(20) @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sortBy
    ){

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        log.info("Request get user list");
        PageResponse<?> data = vehicleControlService.getAllVehiclesWithSortBy(pageNo,pageSize,sortBy);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<?> response = new TuongCommonResponse<>(trace, now, rs , data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-with-sort-by-multiple-columns")
    public ResponseEntity<TuongCommonResponse<?>> getAllVehiclesWithSortByMultipleColumns(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @Min(20) @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String... sortBy
            ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        log.info("Request get user list");
        PageResponse<?> data = vehicleControlService.getAllVehiclesWithSortByMultipleColumns(pageNo,pageSize,sortBy);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<?> response = new TuongCommonResponse<>(trace, now, rs , data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list-with-sort-by-multiple-columns-search")
    public ResponseEntity<TuongCommonResponse<?>> getAllVehiclesWithSortByMultipleColumnsAndSearch(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @Min(20) @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String... sortBy
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        log.info("Request get user list");
        PageResponse<?> data = vehicleControlService.getAllVehiclesWithSortByMultipleColumns(pageNo,pageSize,sortBy);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<?> response = new TuongCommonResponse<>(trace, now, rs , data);
        return ResponseEntity.ok(response);
    }
}
