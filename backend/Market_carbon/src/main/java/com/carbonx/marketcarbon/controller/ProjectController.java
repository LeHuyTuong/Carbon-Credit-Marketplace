package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.service.ProjectService;
import com.carbonx.marketcarbon.service.VehicleService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @Operation(summary = "Create Project for Admin", description = "Create Project")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<Void>> create(
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        projectService.createProject(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace,now,rs,null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Project" , description = "Update Information of Project")
    @PutMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        projectService.updateProject(id,req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace,now,rs,null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Project", description = "Delete Project")
    @DeleteMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
        projectService.deleteProject(id);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace,now,rs,null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Projct" , description = "API Get Project by id")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<ProjectDetailResponse>>> getProjectById(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

            String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
            String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

            List<ProjectDetailResponse> list = projectService.findAllProject();

            TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                    StatusCode.SUCCESS.getMessage());
            TuongCommonResponse<List<ProjectDetailResponse>> response = new TuongCommonResponse<>(trace,now,rs,list);
            return  ResponseEntity.ok(response);
    }
}
