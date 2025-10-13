// CvaController.java
package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.ProjectReviewRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.service.CvaService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
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
@RequestMapping(path = "/api/v1/cva/projects", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('CVA')")
public class CvaController {

    private final CvaService cvaService;

    @Operation(summary = "Review project (CVA)", description = "CVA reviews a project and approves or rejects it.")
    @PostMapping(value = "/review", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> review(
            @Valid @RequestBody TuongCommonRequest<@Valid ProjectReviewRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace
                : (req.getRequestTrace() != null ? req.getRequestTrace() : UUID.randomUUID().toString());
        String now = requestDateTime != null ? requestDateTime
                : (req.getRequestDateTime() != null ? req.getRequestDateTime() : OffsetDateTime.now(ZoneOffset.UTC).toString());

        ProjectResponse data = cvaService.review(req.getData());
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "CVA review completed successfully");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }

    @Operation(summary = "CVA inbox", description = "assignedOnly=true: only projects assigned to current CVA; false: unassigned pool. Filters PENDING_REVIEW and UNDER_REVIEW.")
    @GetMapping("/inbox")
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse>>> cvaInbox(
            @RequestParam(name = "assignedOnly", defaultValue = "true") boolean assignedOnly,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = cvaService.cvaInbox(assignedOnly, pageable);
        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Fetched CVA inbox");
        return ResponseEntity.ok(new TuongCommonResponse<>(trace, now, rs, data));
    }
}
