package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.ProjectService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
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
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> create(
            @ModelAttribute @Valid ProjectRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = projectService.createProject(req);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Create project successfully");
        TuongCommonResponse<ProjectResponse> response = new TuongCommonResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Project Information (Admin only)")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TuongCommonResponse<Void>> update(
            @PathVariable("id") Long id,
            @ModelAttribute @Valid ProjectRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.updateProject(id, req);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Update project successfully");
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Project by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> delete(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.deleteProject(id);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Project (Summary View)")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<ProjectResponse>>> getAllProjects(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectResponse> list = projectService.listAll();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<ProjectResponse>> response = new TuongCommonResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "List All Projects (Summary View)")
    @GetMapping("/all")
    public ResponseEntity<TuongCommonResponse<List<ProjectResponse>>> listAll(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectResponse> data = projectService.listAll();
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Get Project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> getById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = projectService.getById(id);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

}
