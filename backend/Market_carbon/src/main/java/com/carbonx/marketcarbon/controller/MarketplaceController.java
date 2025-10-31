package com.carbonx.marketcarbon.controller;

import com.carbonx.marketcarbon.common.StatusCode;
import com.carbonx.marketcarbon.dto.request.CreditListingRequest;
import com.carbonx.marketcarbon.dto.request.CreditListingUpdateRequest;
import com.carbonx.marketcarbon.dto.response.MarketplaceListingResponse;
import com.carbonx.marketcarbon.service.MarketplaceService;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonRequest;
import com.carbonx.marketcarbon.utils.Tuong.TuongCommonResponse;
import com.carbonx.marketcarbon.utils.Tuong.TuongResponseStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {
    private final MarketplaceService marketplaceService;

    @Operation(summary = "The list credit of Market place" , description = "API to list carbon credits")
    @PostMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<TuongCommonResponse<MarketplaceListingResponse>> listCreditsForSale(
            @Valid @RequestBody TuongCommonRequest<@Valid CreditListingRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        MarketplaceListingResponse data = marketplaceService.listCreditsForSale(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<MarketplaceListingResponse> response = new TuongCommonResponse<>(trace, now , rs , data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "The list credit of Market place" , description = "API to list carbon credits")
    @GetMapping
    public ResponseEntity<TuongCommonResponse<List<MarketplaceListingResponse>>> getActiveListing(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<MarketplaceListingResponse> listings = marketplaceService.getActiveListing();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<MarketplaceListingResponse>> response = new TuongCommonResponse<>(trace, now , rs , listings);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "The list credit of Market place by company " , description = "API to list carbon credits")
    @GetMapping("/company")
    public ResponseEntity<TuongCommonResponse<List<MarketplaceListingResponse>>> getCreditListingByCompany(
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ){
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        List<MarketplaceListingResponse> listings = marketplaceService.getALlCreditListingsByCompanyID();

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<List<MarketplaceListingResponse>> response = new TuongCommonResponse<>(trace, now , rs , listings);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update marketplace listing", description = "API to update listing price")
    @PutMapping
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<TuongCommonResponse<MarketplaceListingResponse>> updateListing(
            @Valid @RequestBody TuongCommonRequest<@Valid CreditListingUpdateRequest> req,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        MarketplaceListingResponse data = marketplaceService.updateListCredits(req.getData());

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<MarketplaceListingResponse> response = new TuongCommonResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel marketplace listing", description = "API to cancel an active listing")
    @DeleteMapping("/{listingId}")
    @PreAuthorize("hasRole('COMPANY')")
    public ResponseEntity<TuongCommonResponse<MarketplaceListingResponse>> cancelListing(
            @PathVariable("listingId") Long listingId,
            @RequestHeader(value = "X-Request-Trace", required = false) String requestTrace,
            @RequestHeader(value = "X-Request-DateTime", required = false) String requestDateTime
    ) {
        String trace = requestTrace != null ? requestTrace : UUID.randomUUID().toString();
        String now = requestDateTime != null ? requestDateTime : OffsetDateTime.now(ZoneOffset.UTC).toString();

        MarketplaceListingResponse data = marketplaceService.deleteListCredits(listingId);

        TuongResponseStatus rs = new TuongResponseStatus(StatusCode.SUCCESS.getCode(),
                StatusCode.SUCCESS.getMessage());
        TuongCommonResponse<MarketplaceListingResponse> response = new TuongCommonResponse<>(trace, now, rs, data);
        return ResponseEntity.ok(response);
    }
}
