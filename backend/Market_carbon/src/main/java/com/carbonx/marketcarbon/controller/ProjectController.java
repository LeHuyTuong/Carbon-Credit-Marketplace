package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.FinalApproveRequest;
import com.carbonx.marketcarbon.dto.request.ProjectRequest;
import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.request.ProjectSubmitRequest;
import com.carbonx.marketcarbon.dto.request.importing.ImportReport;
import com.carbonx.marketcarbon.dto.response.ProjectDetailResponse;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.ProjectService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ==============================================
    // CREATE PROJECT
    // ==============================================
    @Operation(summary = "Create Project (Admin only)")
    @PostMapping
    public ResponseEntity<TuongCommonResponse<Void>> create(
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.createProject(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // UPDATE PROJECT
    // ==============================================
    @Operation(summary = "Update Project Information")
    @PutMapping("/{id}")
    public ResponseEntity<TuongCommonResponse<Void>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        projectService.updateProject(id, req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Void> response = new TuongCommonResponse<>(trace, now, rs, null);
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // DELETE PROJECT
    // ==============================================
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

    // ==============================================
    //  GET ALL PROJECT DETAILS
    // ==============================================
    @Operation(summary = "Get All Project Details")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<ProjectDetailResponse>>> getAllProjects(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<ProjectDetailResponse> list = projectService.findAllProject();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<ProjectDetailResponse>> response = new TuongCommonResponse<>(trace, now, rs, list);
        return ResponseEntity.ok(response);
    }

    // ==============================================
    // SUBMIT PROJECT
    // ==============================================
    @Operation(summary = "Submit Project (Company)")
    @PostMapping("/submit")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> submit(
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectSubmitRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace
                : (req.getRequestTrace() != null ? req.getRequestTrace() : UUID.randomUUID().toString());
        String now = requestDateTime != null ? requestDateTime
                : (req.getRequestDateTime() != null ? req.getRequestDateTime() : OffsetDateTime.now(ZoneOffset.UTC).toString());

        ProjectResponse data = projectService.submit(req.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    // ==============================================
    // SEND TO REVIEW
    // ==============================================
    @Operation(summary = "Send Project to Review (Company)")
    @PostMapping("/{id}/send-to-review")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> sendToReview(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = projectService.sendToReview(id);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    // ==============================================
    // 7REVIEW PROJECT
    // ==============================================
    @Operation(summary = "CVA thẩm định hồ sơ dự án", description = "CVA xem xét hồ sơ, duyệt hoặc từ chối theo tiêu chuẩn quốc gia/quốc tế")
    @PostMapping("/review")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> review(
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectReviewRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace
                : (req.getRequestTrace() != null ? req.getRequestTrace() : UUID.randomUUID().toString());
        String now = requestDateTime != null ? requestDateTime
                : (req.getRequestDateTime() != null ? req.getRequestDateTime()
                : OffsetDateTime.now(ZoneOffset.UTC).toString());

        ProjectResponse data = projectService.review(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(
                StatusCode.SUCCESS.getCode(),
                "CVA review completed successfully"
        );

        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    // ==============================================
    // 8️⃣ LIST ALL PROJECTS (Simplified View)
    // ==============================================
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

    // ==============================================
    //  GET PROJECT BY ID
    // ==============================================
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

    // ==============================================
    // IMPORT CSV
    // ==============================================
    @Operation(summary = "Import CSV (Company register for existing projects)")
    @PostMapping("/import-csv")
    public ResponseEntity<TuongCommonResponse<ImportReport>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ImportReport result = projectService.importCsv(file);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<ImportReport> response = new TuongCommonResponse<>(trace, now, rs, result);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Admin final approval for project")
    @PostMapping("/{id}/final-approve")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> finalApprove(
            @PathVariable Long id,
            @Valid @RequestBody FinalApproveRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        //  chỉ còn 3 tham số
        ProjectResponse data = projectService.finalApprove(id, req.getReviewer(), req.getStatus());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                req.getStatus() == ProjectStatus.ADMIN_APPROVED
                        ? "Admin approved project"
                        : "Admin rejected project");

        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }
}
