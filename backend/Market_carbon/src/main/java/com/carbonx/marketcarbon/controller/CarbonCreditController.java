//package com.carbonx.marketcarbon.controller;
//
//import com.carbonx.marketcarbon.common.ProjectStatus;
//import com.carbonx.marketcarbon.common.StatusCode;
//import com.carbonx.marketcarbon.dto.request.CreditIssuanceRequest;
//import com.carbonx.marketcarbon.dto.response.ProjectResponse;
//import com.carbonx.marketcarbon.exception.WalletException;
//import com.carbonx.marketcarbon.model.CarbonCredit;
//import com.carbonx.marketcarbon.service.CarbonCreditService;
//import com.carbonx.marketcarbon.utils.common.ApiRequest;
//import com.carbonx.marketcarbon.utils.common.ApiResponse;
//import com.carbonx.marketcarbon.utils.common.ResponseStatus;
//import io.swagger.v3.oas.annotations.Operation;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.web.PageableDefault;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("api/v1/carbonCredit")
//@RequiredArgsConstructor
//public class CarbonCreditController {
//    private final CarbonCreditService carbonCreditService;
//
//    @Operation(summary = "Company requests carbon credit issuance ",
//            description = "Endpoint for a Company to request credit issuance from charging data. The system will calculate and create a new credit batch with 'PENDING' status for Admin approval.")
//    @PostMapping("/issue")
//    public ResponseEntity<ApiResponse<CarbonCredit>> issueCredits(
//            @Valid @RequestBody ApiRequest<CreditIssuanceRequest> request,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//            ) throws WalletException {
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        CarbonCredit carbonCredit = carbonCreditService.issueCredits(request.getData());
//
//        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
//                StatusCode.SUCCESS.getMessage());
//        ApiResponse<CarbonCredit> resp = new ApiResponse<>(trace, now , rs, carbonCredit);
//        return ResponseEntity.ok(resp);
//    }
//
//    @Operation(summary = "Admin approves a carbon credit batch" ,
//            description = "Upon approval, the credit batch status will change to 'APPROVED', and the corresponding credit amount will be added to the owner company's wallet.")
//    @PostMapping("{creditId}/approve")
//    public ResponseEntity<ApiResponse<CarbonCredit>> approveDataOfProject(
//            @PathVariable @Valid Long creditId,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//            ) throws WalletException {
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        CarbonCredit approveCredit = carbonCreditService.approveCarbonCredit(creditId);
//
//        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
//                StatusCode.SUCCESS.getMessage());
//        ApiResponse<CarbonCredit> resp = new ApiResponse<>(trace, now , rs, approveCredit);
//        return ResponseEntity.ok(resp);
//    }
//
//    @Operation(
//            summary = "Admin final approves or rejects a project",
//            description = "Endpoint for an Admin to give the final approval or rejection for a project that has already been approved by a CVA."
//    )
//    @PostMapping("/{projectId}/final-review")
//    public ResponseEntity<ApiResponse<ProjectResponse>> finalApproveProject(
//            @PathVariable Long projectId,
//            @RequestParam ProjectStatus status,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime) {
//
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        ProjectResponse projectResponse = carbonCreditService.finalApprove(projectId, status);
//
//        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(), "Project final review completed successfully.");
//        ApiResponse<ProjectResponse> resp = new ApiResponse<>(trace, now, rs, projectResponse);
//        return ResponseEntity.ok(resp);
//    }
//
//
//    @Operation(
//            summary = "Admin gets their inbox of projects to review",
//            description = "Retrieves a paginated list of all projects with status 'CVA_APPROVED', which are pending final review from the Admin"
//    )
//    @GetMapping("/pending")
//    public ResponseEntity<ApiResponse<Page<ProjectResponse> >> getAdminInbox(
//            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//    ) throws WalletException {
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        Page<ProjectResponse> data = carbonCreditService.adminInbox(pageable);
//
//        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
//                StatusCode.SUCCESS.getMessage());
//        ApiResponse<Page<ProjectResponse> > resp = new ApiResponse<>(trace, now , rs, data);
//        return ResponseEntity.ok(resp);
//    }
//
//    @Operation(
//            summary = "Admin lists projects reviewed by a specific CVA",
//            description = "Retrieves a paginated list of projects (both approved and rejected) that were reviewed by a specific CVA, identified by their name."
//    )
//    @GetMapping
//    public ResponseEntity<ApiResponse<Page<ProjectResponse> >> listProjectsReviewedByCva(
//            @RequestParam Long reviewerId,
//            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
//            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
//            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
//    ) throws WalletException {
//        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
//        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();
//
//        Page<ProjectResponse> data = carbonCreditService.adminListReviewedByCva(reviewerId, pageable);
//
//        ResponseStatus rs = new ResponseStatus(StatusCode.SUCCESS.getCode(),
//                StatusCode.SUCCESS.getMessage());
//        ApiResponse<Page<ProjectResponse> > resp = new ApiResponse<>(trace, now , rs, data);
//        return ResponseEntity.ok(resp);
//    }
//
//}
