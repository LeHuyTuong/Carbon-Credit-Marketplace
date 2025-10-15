// AdminController.java
package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.FinalApproveRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.AdminService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/v1/admin/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Final approval (Admin)", description = "Admin issues the final decision for a CVA approved project.")
    @PostMapping(value = "/{id}/final-approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> finalApprove(
            @PathVariable Long id,
            @Valid @RequestBody FinalApproveRequest req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse data = adminService.finalApprove(id, req.getStatus());
        String msg = (req.getStatus() == ProjectStatus.ADMIN_APPROVED) ? "Admin approved project" : "Admin rejected project";
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), msg);
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Projects reviewed by a CVA (Admin)", description = "Lists CVA-approved and rejected projects for audit and oversight.")
    @GetMapping("/reviewed-by-cva/{cvaId}")
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse>>> adminListReviewedByCva(
            @PathVariable("cvaId") Long cvaId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = adminService.adminListReviewedByCva(cvaId, pageable);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Fetched projects reviewed by CVA");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "Admin Check (CVA-approved)", description = "Lists projects in CVA_APPROVED state awaiting final decision.")
    @GetMapping("/cva-approved")
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse>>> adminCheck(
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = adminService.adminInbox(pageable);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Fetched admin inbox");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }
}
