package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.request.ProjectUpdateRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.ProjectService;
import com.carbonx.marketcarbon.utils.common.ApiRequest;
import com.carbonx.marketcarbon.utils.common.ApiResponse;
import com.carbonx.marketcarbon.utils.common.ResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Project (Admin only)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @ModelAttribute @Valid ProjectRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = projectService.createProject(req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Create project successfully");
        ApiResponse<ProjectResponse> response = new ApiResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Project Information (Admin only)")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable("id") Long id,
            @ModelAttribute @Valid ProjectUpdateRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.updateProject(id, req);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Update project successfully");
        ApiResponse<Void> response = new ApiResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Project by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.deleteProject(id);

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<Void> response = new ApiResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Project (Summary View)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectResponse> list = projectService.listAll();

        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        ApiResponse<List<ProjectResponse>> response = new ApiResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "List All Projects (Summary View)")
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listAll(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectResponse> data = projectService.listAll();
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get Project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = projectService.getById(id);
        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new ApiResponse<>(trace, now, rs, data));
    }

}
