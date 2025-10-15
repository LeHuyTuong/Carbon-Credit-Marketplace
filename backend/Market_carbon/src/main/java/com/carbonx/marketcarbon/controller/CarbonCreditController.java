package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.ProjectStatus;
import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.CreditIssuanceRequest;
import com.carbonx.marketcarbon.dto.response.ProjectResponse;
import com.carbonx.marketcarbon.exception.WalletException;
import com.carbonx.marketcarbon.model.CarbonCredit;
import com.carbonx.marketcarbon.service.CarbonCreditService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/carbonCredit")
@RequiredArgsConstructor
public class CarbonCreditController {
    private final CarbonCreditService carbonCreditService;

    @Operation(summary = "Company requests carbon credit issuance ",
            description = "Endpoint for a Company to request credit issuance from charging data. The system will calculate and create a new credit batch with 'PENDING' status for Admin approval.")
    @PostMapping("/issue")
    public ResponseEntity<TuongCommonResponse<CarbonCredit>> issueCredits(
            @Valid @RequestBody TuongCommonRequest<CreditIssuanceRequest> request,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) throws WalletException {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CarbonCredit carbonCredit = carbonCreditService.issueCredits(request.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<CarbonCredit> resp = new TuongCommonResponse<>(trace, now , rs, carbonCredit);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Admin approves a carbon credit batch" ,
            description = "Upon approval, the credit batch status will change to 'APPROVED', and the corresponding credit amount will be added to the owner company's wallet.")
    @PostMapping("{creditId}/approve")
    public ResponseEntity<TuongCommonResponse<CarbonCredit>> approveDataOfProject(
            @PathVariable @Valid Long creditId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
            ) throws WalletException {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        CarbonCredit approveCredit = carbonCreditService.approveCarbonCredit(creditId);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<CarbonCredit> resp = new TuongCommonResponse<>(trace, now , rs, approveCredit);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Admin final approves or rejects a project",
            description = "Endpoint for an Admin to give the final approval or rejection for a project that has already been approved by a CVA."
    )
    @PostMapping("/{projectId}/final-review")
    public ResponseEntity<TuongCommonResponse<ProjectResponse>> finalApproveProject(
            @PathVariable Long projectId,
            @RequestParam ProjectStatus status,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {

        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        ProjectResponse projectResponse = carbonCreditService.finalApprove(projectId, status);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(), "Project final review completed successfully.");
        TuongCommonResponse<ProjectResponse> resp = new TuongCommonResponse<>(trace, now, rs, projectResponse);
        return ResponseEntity.ok(resp);
    }


    @Operation(
            summary = "Admin gets their inbox of projects to review",
            description = "Retrieves a paginated list of all projects with status 'CVA_APPROVED', which are pending final review from the Admin"
    )
    @GetMapping("/pending")
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse> >> getAdminInbox(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) throws WalletException {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = carbonCreditService.adminInbox(pageable);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Page<ProjectResponse> > resp = new TuongCommonResponse<>(trace, now , rs, data);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Admin lists projects reviewed by a specific CVA",
            description = "Retrieves a paginated list of projects (both approved and rejected) that were reviewed by a specific CVA, identified by their name."
    )
    @GetMapping
    public ResponseEntity<TuongCommonResponse<Page<ProjectResponse> >> listProjectsReviewedByCva(
            @RequestParam Long reviewerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) throws WalletException {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        Page<ProjectResponse> data = carbonCreditService.adminListReviewedByCva(reviewerId, pageable);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<Page<ProjectResponse> > resp = new TuongCommonResponse<>(trace, now , rs, data);
        return ResponseEntity.ok(resp);
    }

}
