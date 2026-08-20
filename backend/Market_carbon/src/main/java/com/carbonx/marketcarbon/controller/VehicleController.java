package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.VehicleCreateRequest;
import com.carbonx.marketcarbon.dto.request.VehicleUpdateRequest;
import com.carbonx.marketcarbon.dto.response.CompanyVehicleSummaryResponse;
import com.carbonx.marketcarbon.dto.response.PageResponse;
import com.carbonx.marketcarbon.dto.response.VehicleResponse;
import com.carbonx.marketcarbon.exception.AppException;
import com.carbonx.marketcarbon.exception.ErrorCode;
import com.carbonx.marketcarbon.exception.ResourceNotFoundException;
import com.carbonx.marketcarbon.service.VehicleControlService;
import com.carbonx.marketcarbon.service.VehicleService;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    private final VehicleControlService vehicleControlService;


    @Operation(summary = "Create Vehicle (EV Owner)", description = "API for EV Owner to register vehicle with image/document (uploaded to S3)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @ModelAttribute @Valid VehicleCreateRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        if (req.getDocumentFile() == null || req.getDocumentFile().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_FILE);
        }

        VehicleResponse created = vehicleService.create(req);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicle created successfully");

        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, created));
    }


    @Operation(summary = "Get all my vehicles (EV Owner)", description = "Get list of vehicles owned by current EV Owner")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        List<VehicleResponse> vehicles = vehicleService.getOwnerVehicles();
        if (vehicles == null || vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No vehicles found for this owner", requestTrace, requestDateTime);
        }

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicles fetched successfully");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, vehicles));
    }


    @Operation(summary = "Update vehicle", description = "Update vehicle info, upload new document/image if needed")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable("id") Long id,
            @ModelAttribute @Valid VehicleUpdateRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        VehicleResponse updated = vehicleService.update(id, req);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicle updated successfully");

        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, updated));
    }


    @Operation(summary = "Delete vehicle", description = "API Delete vehicle")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        vehicleService.delete(id);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicle deleted successfully");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, null));
    }


    @Operation(summary = "Count my vehicles (EV Owner)", description = "Count vehicles registered by the logged-in EV Owner")
    @GetMapping("/my/count")
    public ResponseEntity<ApiResponse<Long>> countMyVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        long count = vehicleService.countMyVehicles();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Count fetched successfully");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, count));
    }


    @Operation(summary = "Count all vehicles (Admin)", description = "Count total vehicles for all users/companies")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countAllVehicles(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        long count = vehicleControlService.getTotalVehicleCount();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Total count fetched successfully");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, count));
    }

    @Operation(summary = "Get paginated vehicle list", description = "Paginated list of vehicles with sorting")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<?>>> getVehicleList(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @Min(20) @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sortBy
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PageResponse<?> data = vehicleControlService.getAllVehiclesWithSortBy(pageNo, pageSize, sortBy);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicles list fetched");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get vehicles of a company", description = "Get paginated vehicle list by company")
    @GetMapping("/list-by-company")
    public ResponseEntity<ApiResponse<PageResponse<?>>> getVehiclesByCompany(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime,
            @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
            @Min(20) @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "sort", required = false) String sortBy
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        PageResponse<?> data = vehicleControlService.getAllVehiclesOfCompanyWithSortBy(pageNo, pageSize, sortBy);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Vehicles by company fetched");
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get company vehicle summary", description = "Company views all EV Owners and their registered vehicles")
    @GetMapping("/company/summary")
    public ResponseEntity<ApiResponse<List<CompanyVehicleSummaryResponse>>> getCompanyVehicleSummary(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = (requestTrace != null) ? requestTrace : UUID.randomUUID().toString();
        String now = (requestDateTime != null) ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<CompanyVehicleSummaryResponse> data = vehicleService.getCompanyVehicleSummary();

        ResponseStatus rs = new ResponseStatus(
                StatusCode.SUCCESS.getCode(),
                "Company vehicle summary fetched successfully"
        );

        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, data));
    }


}
